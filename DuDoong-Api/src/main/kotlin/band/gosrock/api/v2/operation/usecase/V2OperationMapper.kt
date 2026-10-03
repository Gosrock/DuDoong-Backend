package band.gosrock.api.v2.operation.usecase

import band.gosrock.api.v2.operation.dto.V2RefundStatus
import band.gosrock.api.v2.operation.dto.response.V2CheckInResponse
import band.gosrock.api.v2.operation.dto.response.V2CheckInTicketResponse
import band.gosrock.api.v2.operation.dto.response.V2IssuedTicketElement
import band.gosrock.api.v2.operation.dto.response.V2OptionAnswerResponse
import band.gosrock.api.v2.operation.dto.response.V2OrderElement
import band.gosrock.api.v2.operation.dto.response.V2RefundElement
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInOutcome
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
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
        )
    }

    fun toRefundElement(order: Order, user: User?) = V2RefundElement(
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
    )

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

    /** (optionId, answer, additionalPrice) → 옵션 이름 포함 응답. 옵션 이름은 한 번에 조회 */
    fun toOptionAnswers(answers: List<Triple<Long?, String?, Long>>): List<V2OptionAnswerResponse> {
        val options = optionAdaptor.findAllByIds(answers.mapNotNull { it.first }.distinct()).associateBy { it.id }
        return answers.map { (optionId, answer, price) ->
            V2OptionAnswerResponse(optionName = options[optionId]?.getQuestionName(), answer = answer, additionalPrice = price)
        }
    }

    fun ticketOptionAnswers(ticket: IssuedTicket): List<V2OptionAnswerResponse> =
        toOptionAnswers(ticket.issuedTicketOptionAnswers.sortedBy { it.id }.map { Triple(it.optionId, it.answer, it.additionalPrice.longValue()) })

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
