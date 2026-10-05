package band.gosrock.api.v2.operation.usecase

import band.gosrock.api.v2.operation.dto.V2RefundStatus
import band.gosrock.api.v2.operation.dto.response.V2CheckInResponse
import band.gosrock.api.v2.operation.dto.response.V2CheckInTicketResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketElement
import band.gosrock.api.v2.operation.dto.response.V2OptionAnswerResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderElement
import band.gosrock.api.v2.operation.dto.response.V2RefundAccountResponse
import band.gosrock.api.v2.operation.dto.response.V2RefundElement
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInOutcome
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2HostGiftState
import band.gosrock.domain.domains.issuedTicket.service.v2.V2IssuedTicketQuery
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import org.springframework.stereotype.Component

/** 발급 티켓 한 장의 응답 + 연락처용 주문자·소유자 회원 (#740) */
data class V2IssuedTicketView(val ticket: IssuedTicket, val element: V2IssuedTicketElement, val buyer: User?, val owner: User?)

/** 엑셀 옵션 컬럼: [groupIds] 순서가 [headers] 순서. [groupOfOption] = 답변의 옵션 행 id → 옵션 그룹 id */
data class V2ExcelOptionColumns(val groupIds: List<Long>, val headers: List<String>, val groupOfOption: Map<Long?, Long?>)

/**
 * v2 공연 운영 응답 변환 (#712). 트랜잭션 안(UseCase)에서 호출한다 (옵션 그룹 지연 로딩).
 *
 * 개인정보 범위: 연락처는 주문 목록·상세·발급 티켓 상세·엑셀, 이메일은 주문 상세에만. 마스킹 없음 —
 * v1 어드민 주문 목록(일반 멤버 이상)이 이름·연락처·이메일 전체를 주므로 v1 범위 안이고, 입금 확인·연락에 필요하다
 */
@Component
class V2OperationMapper(
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val orderAdaptor: OrderAdaptor,
    private val optionAdaptor: OptionAdaptor,
    private val v2IssuedTicketQuery: V2IssuedTicketQuery,
) {
    fun usersOf(userIds: Collection<Long?>): Map<Long, User> =
        userAdaptor.findUserByIdIn(userIds.filterNotNull().distinct()).associateBy { it.id!! }

    fun toOrderElement(order: Order, user: User?): V2OrderElement {
        val status = V2OrderStatus.of(order)
        return V2OrderElement(
            orderUuid = order.uuid!!,
            orderNo = order.orderNo,
            buyerName = user?.profile?.name,
            buyerPhone = phoneOf(user),
            ticketName = order.orderName,
            totalQuantity = order.getTotalQuantity(),
            totalPaymentAmount = order.getTotalPaymentPrice().longValue(),
            orderedAt = order.createdAt,
            status = status,
            refundStatus = V2RefundStatus.of(order.refundStatus),
            refuseReasonType = order.refuseReasonType,
            refuseReason = order.cancelReason.takeIf { status == V2OrderStatus.REFUSED },
            cancelReason = order.cancelReason.takeIf { status == V2OrderStatus.CANCELED },
            paymentChannel = order.paymentChannel,
            depositorName = order.depositorName,
        )
    }

    fun toRefundElement(order: Order, user: User?, refundAccount: OrderRefundAccount?) = V2RefundElement(
        orderUuid = order.uuid,
        orderNo = order.orderNo,
        buyerName = user?.profile?.name,
        ticketName = order.orderName,
        totalPaymentAmount = order.getTotalPaymentPrice().longValue(),
        status = V2OrderStatus.of(order),
        refundStatus = V2RefundStatus.of(order.refundStatus),
        reason = order.cancelReason,
        withdrawnAt = order.withDrawAt,
        refundStatusChangedAt = order.refundStatusChangedAt,
        refundAccount = refundAccount?.let(::toRefundAccount),
    )

    fun toRefundAccount(account: OrderRefundAccount) =
        V2RefundAccountResponse(bankName = account.bankName, accountHolder = account.accountHolder, accountNumber = account.accountNumber, updatedAt = account.updatedAt)

    /**
     * 사용자 환불 계좌를 볼 수 있는지 (#718): 공연 호스트의 활성 마스터·매니저 또는 SUPER_ADMIN.
     * 일반 멤버(G+ 조회 API)에게는 숨긴다 — 송금·환불 완료(F-2)가 매니저 이상 권한이라 계좌도 그 범위만 (DEC-009 정신)
     */
    fun canSeeRefundAccount(userId: Long, eventId: Long): Boolean {
        if (userAdaptor.queryUser(userId).accountRole == AccountRole.SUPER_ADMIN) return true
        val host = hostAdaptor.findById(eventAdaptor.findById(eventId).hostId!!)
        return host.getActiveRoleOf(userId).let { it == HostRole.MASTER || it == HostRole.MANAGER }
    }

    /**
     * 발급 티켓 응답(I-1·I-2·I-3) 변환 (#740). 주문(주문번호·주문자)·회원(주문자·소유자)·선물 상태를 티켓 수와 관계없이 한 번씩 일괄 조회한다.
     * 주문자 = 주문 사용자, 소유자 = 지금 티켓을 가진 사용자(`issued_ticket.user_id`, 선물 수락 시 받은 사람)
     */
    fun ticketViewsOf(tickets: List<IssuedTicket>): List<V2IssuedTicketView> {
        if (tickets.isEmpty()) return emptyList()
        val orders = orderAdaptor.findByUuidIn(tickets.mapNotNull { it.orderUuid }.distinct()).associateBy { it.uuid!! }
        val users = usersOf(tickets.map { it.getUserId() } + orders.values.map { it.userId })
        val giftStates = v2IssuedTicketQuery.giftStatesOf(tickets.mapNotNull { it.id })
        return tickets.map { ticket ->
            val order = ticket.orderUuid?.let { orders[it] }
            val owner = users[ticket.getUserId()]
            val buyer = order?.userId?.let { users[it] }
            val ownerName = owner?.profile?.name ?: ticket.userInfo?.userName
            V2IssuedTicketView(
                ticket = ticket,
                buyer = buyer,
                owner = owner,
                element = V2IssuedTicketElement(
                    ticketUuid = ticket.uuid,
                    issuedTicketNo = ticket.issuedTicketNo,
                    ticketItemId = ticket.itemInfo?.ticketItemId,
                    payType = V2TicketPayType.of(ticket.itemInfo?.payType),
                    ticketName = ticket.itemInfo?.ticketName,
                    // 주문이 없으면(방어) 소유자 이름으로 대신한다
                    buyerName = if (order == null) ownerName else buyer?.profile?.name,
                    ownerName = ownerName,
                    giftState = giftStates[ticket.id] ?: V2HostGiftState.NONE,
                    orderUuid = ticket.orderUuid,
                    orderNo = order?.orderNo,
                    issuedAt = ticket.createdAt,
                    entrance = V2EntranceState.of(ticket.issuedTicketStatus),
                    enteredAt = ticket.enteredAt,
                ),
            )
        }
    }

    /**
     * 엑셀 옵션 컬럼 (I-3 발급 티켓·R-6 주문 공통 규칙, #730). 답변에 나온 옵션 행 id 들을 한 번에 조회해 옵션 그룹 단위 컬럼을 만든다:
     * 옵션 그룹 id 순, 헤더는 질문 이름. 이름이 없거나, 다른 옵션 이름 또는 **그 엑셀의 기본 열**([baseHeaders], 예: R-6 '입금자명')과 겹치면 `이름(그룹 id)`.
     * 열 집합은 엑셀마다 대상·필터가 달라 다를 수 있다 (답변에 나온 옵션만 — soft delete 된 옵션도 지난 답변이 있으면 열이 남는다).
     * 그래도 헤더가 겹치면(예: 질문 이름이 원래 `뒷풀이(3)`) 겹치지 않을 때까지 `(그룹 id)` 를 한 번 더 붙인다
     */
    fun excelOptionColumnsOf(optionIds: Collection<Long?>, baseHeaders: List<String>): V2ExcelOptionColumns {
        val ids = optionIds.filterNotNull().distinct()
        val options = if (ids.isEmpty()) emptyList() else optionAdaptor.findAllByIds(ids)
        val groupOfOption = options.associate { it.id to it.getOptionGroupId() }
        val groups = options.mapNotNull { o -> o.getOptionGroupId()?.let { it to o.getQuestionName() } }.distinct().sortedBy { it.first }
        val duplicated = groups.groupBy { it.second }.filterValues { it.size > 1 }.keys
        val taken = baseHeaders.toMutableSet()
        val headers = groups.map { (id, name) ->
            // 기본 열과 겹치는 이름은 아래 반복에서 `(id)` 가 붙는다
            var header = if (name == null || name in duplicated) "${name ?: "옵션"}($id)" else name
            while (header in taken) header = "$header($id)"
            header.also { taken += it }
        }
        return V2ExcelOptionColumns(groupIds = groups.map { it.first }, headers = headers, groupOfOption = groupOfOption)
    }

    /**
     * R-6 주문 한 행의 옵션 셀 (옵션 그룹 id → 셀, #730). 라인이 1개면 응답 그대로, 여러 개(티켓별 옵션 = 수량 1 라인 N개 등)면
     * 같은 응답끼리 수량을 더해 `응답 ×수량` 을 처음 나온 순서로 **줄바꿈**으로 잇는다 (응답 안의 쉼표와 헷갈리지 않게 — 셀은 자동 줄바꿈).
     * 응답 안의 줄바꿈은 공백으로 바꾸고, 빈 응답은 빼고, 수량이 없으면 1로 본다. 응답이 없는 그룹은 맵에 없다(빈 칸)
     */
    fun excelOptionCells(orderLines: List<OrderLineItem>, columns: V2ExcelOptionColumns): Map<Long, String> {
        val lines = orderLines.sortedBy { it.id }
        val answered = LinkedHashMap<Long, LinkedHashMap<String, Long>>()
        lines.forEach { line ->
            line.orderOptionAnswers.forEach { a ->
                val groupId = columns.groupOfOption[a.optionId] ?: return@forEach
                val answer = a.answer?.replace(LINE_BREAK, " ")?.takeIf { it.isNotBlank() } ?: return@forEach
                val quantities = answered.getOrPut(groupId) { LinkedHashMap() }
                quantities[answer] = (quantities[answer] ?: 0L) + (line.quantity ?: 1L)
            }
        }
        return answered.mapValues { (_, quantities) ->
            if (lines.size == 1) quantities.keys.single() else quantities.entries.joinToString("\n") { (answer, quantity) -> "$answer ×$quantity" }
        }
    }

    /** 옵션 행 id → 질문(옵션 그룹) 이름. 여러 답변의 옵션을 한 번에 조회한다 */
    fun optionNamesOf(optionIds: Collection<Long?>): Map<Long?, String?> {
        val ids = optionIds.filterNotNull().distinct()
        if (ids.isEmpty()) return emptyMap()
        return optionAdaptor.findAllByIds(ids).associate { it.id to it.getQuestionName() }
    }

    /** (optionId, answer, additionalPrice) → 옵션 이름 포함 응답. [names] 는 [optionNamesOf] 로 미리 조회한 것 */
    fun toOptionAnswers(answers: List<Triple<Long?, String?, Long>>, names: Map<Long?, String?>): List<V2OptionAnswerResponse> =
        answers.map { (optionId, answer, price) -> V2OptionAnswerResponse(optionName = names[optionId], answer = answer, additionalPrice = price) }

    fun ticketAnswerRows(ticket: IssuedTicket): List<Triple<Long?, String?, Long>> =
        ticket.issuedTicketOptionAnswers.sortedBy { it.id }.map { Triple(it.optionId, it.answer, it.additionalPrice.longValue()) }

    fun ticketOptionAnswers(ticket: IssuedTicket): List<V2OptionAnswerResponse> {
        val rows = ticketAnswerRows(ticket)
        return toOptionAnswers(rows, optionNamesOf(rows.map { it.first }))
    }

    fun toCheckInResponse(outcome: V2CheckInOutcome): V2CheckInResponse {
        val users = usersOf((listOfNotNull(outcome.ticket) + outcome.candidates).map { it.getUserId() })
        fun summary(t: IssuedTicket) = V2CheckInTicketResponse(
            ticketUuid = t.uuid,
            issuedTicketNo = t.issuedTicketNo,
            ticketName = t.itemInfo?.ticketName,
            buyerName = users[t.getUserId()]?.profile?.name ?: t.userInfo?.userName,
            entrance = V2EntranceState.of(t.issuedTicketStatus),
            enteredAt = t.enteredAt,
        )
        return V2CheckInResponse(
            result = outcome.result,
            ticket = outcome.ticket?.let(::summary),
            candidates = outcome.candidates.map(::summary),
        )
    }

    fun phoneOf(user: User?): String? =
        user?.profile?.phoneNumberVo?.takeIf { it.phoneNumber != null }?.let { runCatching { it.getNationalFormat() }.getOrNull() }

    companion object {
        private val LINE_BREAK = Regex("\r\n|\r|\n")
    }
}
