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
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import org.springframework.stereotype.Component

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
) {
    fun usersOf(userIds: Collection<Long?>): Map<Long, User> =
        userAdaptor.findUserByIdIn(userIds.filterNotNull().distinct()).associateBy { it.id!! }

    fun orderNosOf(orderUuids: Collection<String?>): Map<String, String?> =
        if (orderUuids.isEmpty()) emptyMap()
        else orderAdaptor.findByUuidIn(orderUuids.filterNotNull().distinct()).associate { it.uuid!! to it.orderNo }

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
        V2RefundAccountResponse(bankName = account.bankName, accountHolder = account.accountHolder, accountNumber = account.accountNumber)

    /**
     * 사용자 환불 계좌를 볼 수 있는지 (#718): 공연 호스트의 활성 마스터·매니저 또는 SUPER_ADMIN.
     * 일반 멤버(G+ 조회 API)에게는 숨긴다 — 송금·환불 완료(F-2)가 매니저 이상 권한이라 계좌도 그 범위만 (DEC-009 정신)
     */
    fun canSeeRefundAccount(userId: Long, eventId: Long): Boolean {
        if (userAdaptor.queryUser(userId).accountRole == AccountRole.SUPER_ADMIN) return true
        val host = hostAdaptor.findById(eventAdaptor.findById(eventId).hostId!!)
        return host.getActiveRoleOf(userId).let { it == HostRole.MASTER || it == HostRole.MANAGER }
    }

    fun toTicketElement(ticket: IssuedTicket, user: User?, orderNo: String?) = V2IssuedTicketElement(
        ticketUuid = ticket.uuid,
        issuedTicketNo = ticket.issuedTicketNo,
        ticketItemId = ticket.itemInfo?.ticketItemId,
        payType = V2TicketPayType.of(ticket.itemInfo?.payType),
        ticketName = ticket.itemInfo?.ticketName,
        // 현재 회원 이름 (없으면 발급 시점 이름)
        buyerName = user?.profile?.name ?: ticket.userInfo?.userName,
        orderUuid = ticket.orderUuid,
        orderNo = orderNo,
        issuedAt = ticket.createdAt,
        entrance = V2EntranceState.of(ticket.issuedTicketStatus),
        enteredAt = ticket.enteredAt,
    )

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
}
