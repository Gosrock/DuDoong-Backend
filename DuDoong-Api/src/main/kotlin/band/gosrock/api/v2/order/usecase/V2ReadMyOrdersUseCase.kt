package band.gosrock.api.v2.order.usecase

import band.gosrock.api.v2.common.V2PageResponse
import band.gosrock.api.v2.operation.dto.V2RefundStatus
import band.gosrock.api.v2.operation.usecase.V2OperationMapper
import band.gosrock.api.v2.order.dto.response.V2MyOrderAccountResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderDetailResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderElement
import band.gosrock.api.v2.order.dto.response.V2MyOrderEventResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderIssuedTicketResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderLineResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderPaymentResponse
import band.gosrock.api.v2.order.dto.response.V2MyOrderTicketResponse
import band.gosrock.api.v2.order.dto.response.V2MyRefundAccountResponse
import band.gosrock.api.v2.ticket.dto.V2TicketPayType
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.service.v2.V2EventDisplayRule
import band.gosrock.domain.domains.gift.service.v2.V2GiftState
import band.gosrock.domain.domains.gift.service.v2.V2TicketGiftDomainService
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.issuedTicket.service.v2.V2EntranceState
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.service.v2.V2MyOrderStatus
import band.gosrock.domain.domains.order.service.v2.V2OrderStatus
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import band.gosrock.domain.domains.order.service.v2.V2UserOrderQuery
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository
import java.time.LocalDateTime
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

/** O-2 내 주문 목록 / O-3 내 주문 상세 (본인 주문만, 남의 주문은 404) */
@UseCase
class V2ReadMyOrdersUseCase(
    private val eventAdaptor: EventAdaptor,
    private val issuedTicketAdaptor: IssuedTicketAdaptor,
    private val ticketItemRepository: TicketItemRepository,
    private val v2UserOrderQuery: V2UserOrderQuery,
    private val v2UserOrderDomainService: V2UserOrderDomainService,
    private val mapper: V2OperationMapper,
    private val giftDomainService: V2TicketGiftDomainService,
) {

    /** O-2 최신 순. status null 이면 전체(결제 진행 중·실패 주문 제외 — v1 마이페이지 목록과 같은 기준) */
    @Transactional(readOnly = true)
    fun execute(userId: Long, status: V2MyOrderStatus?, page: Int, size: Int): V2PageResponse<V2MyOrderElement> {
        val orders = v2UserOrderQuery.findMyOrders(userId, status, PageRequest.of(page, size))
        val events = eventsOf(orders.content.mapNotNull { it.eventId })
        val now = LocalDateTime.now()
        return V2PageResponse.of(
            orders.map { order ->
                V2MyOrderElement(
                    orderUuid = order.uuid!!,
                    orderNo = order.orderNo,
                    event = events[order.eventId]?.let { eventResponse(it, now) },
                    ticketName = order.orderName,
                    quantity = order.getTotalQuantity(),
                    totalAmount = order.getTotalPaymentPrice().longValue(),
                    status = V2MyOrderStatus.of(order),
                    refundStatus = V2RefundStatus.of(order.refundStatus),
                    orderedAt = order.createdAt,
                )
            },
        )
    }

    /** O-3. 변경 API(O-1·O-4) 응답도 이것으로, 변경 트랜잭션 커밋 뒤에 읽는다 */
    @Transactional(readOnly = true)
    fun detail(userId: Long, orderUuid: String): V2MyOrderDetailResponse {
        val order = v2UserOrderDomainService.queryMyOrder(userId, orderUuid)
        val event = eventAdaptor.findById(order.eventId!!)
        val now = LocalDateTime.now()
        val lines = order.orderLineItems.sortedBy { it.id }
        val firstLine = lines.firstOrNull()
        val item = firstLine?.orderItem?.itemId?.let { ticketItemRepository.findById(it).orElse(null) }
        val lineAnswers = lines.associate { line ->
            line.id to line.orderOptionAnswers.sortedBy { it.id }.map { Triple(it.optionId, it.answer, it.additionalPrice.longValue()) }
        }
        // 본인 소유 티켓 + 내가 선물해 수락된 티켓('선물 완료' 행, uuid·QR 없음 — DEC-026 #3, #719)
        val orderTickets = issuedTicketAdaptor.findAllByOrderUuid(orderUuid)
        val latestGifts = giftDomainService.latestGiftsOf(orderTickets.mapNotNull { it.id })
        val giftStates = orderTickets.associate { it.id to giftDomainService.giftStateOf(it, order.userId, latestGifts[it.id], userId) }
        val tickets = orderTickets.filter { it.getUserId() == userId || giftStates[it.id] == V2GiftState.SENT }.sortedBy { it.id }
        val ticketAnswers = tickets.associate { it.id to mapper.ticketAnswerRows(it) }
        val names = mapper.optionNamesOf((lineAnswers.values + ticketAnswers.values).flatten().map { it.first })
        val v2Status = V2OrderStatus.of(order)
        val paid = order.getTotalPaymentPrice().isGreaterThan(Money.ZERO)
        val refundAccount = order.id?.let { v2UserOrderDomainService.refundAccountOf(it) }
        return V2MyOrderDetailResponse(
            orderUuid = order.uuid!!,
            orderNo = order.orderNo,
            status = V2MyOrderStatus.of(order),
            refundStatus = V2RefundStatus.of(order.refundStatus),
            orderedAt = order.createdAt,
            approvedAt = order.approvedAt,
            withdrawnAt = order.withDrawAt,
            refundStatusChangedAt = order.refundStatusChangedAt,
            event = eventResponse(event, now),
            ticket = V2MyOrderTicketResponse(
                ticketItemId = firstLine?.orderItem?.itemId,
                name = firstLine?.orderItem?.name ?: order.orderName,
                payType = V2TicketPayType.of(item?.payType),
                unitPrice = firstLine?.orderItem?.price?.longValue() ?: 0L,
            ),
            approvalRequired = order.orderMethod == OrderMethod.APPROVAL,
            quantity = order.getTotalQuantity(),
            paymentChannel = order.paymentChannel,
            depositorName = order.depositorName,
            payment = if (!paid) null else V2MyOrderPaymentResponse(
                ticketAmount = lines.sumOf { it.getItemPrice().longValue() * (it.quantity ?: 0L) },
                optionAmount = lines.sumOf { it.getOptionAnswersPrice().longValue() * (it.quantity ?: 0L) },
                discountAmount = order.getTotalDiscountPrice().longValue(),
                totalAmount = order.getTotalPaymentPrice().longValue(),
                // 계좌는 승인형(두둥티켓) 주문만. 카드(PG) 주문은 없음
                account = item?.accountInfo?.takeIf { order.orderMethod == OrderMethod.APPROVAL }?.let {
                    V2MyOrderAccountResponse(bankName = it.bankName, accountHolder = it.accountHolder, accountNumber = it.accountNumber)
                },
            ),
            lines = lines.map { line ->
                V2MyOrderLineResponse(
                    quantity = line.quantity ?: 0L,
                    linePrice = line.getTotalOrderLinePrice().longValue(),
                    optionAnswers = mapper.toOptionAnswers(lineAnswers.getValue(line.id), names),
                )
            },
            refuseReasonType = order.refuseReasonType.takeIf { v2Status == V2OrderStatus.REFUSED },
            refuseReason = order.cancelReason.takeIf { v2Status == V2OrderStatus.REFUSED },
            cancelReason = order.cancelReason.takeIf { V2MyOrderStatus.of(order) == V2MyOrderStatus.CANCELED },
            refundAccount = refundAccount?.let {
                V2MyRefundAccountResponse(bankName = it.bankName, accountHolder = it.accountHolder, maskedAccountNumber = it.maskedAccountNumber())
            },
            // 환불 계좌 입력·수정 (#728): v2 주문 중 환불 요청 중인 유료 계좌이체 주문 (거절·호스트 취소·사용자 취소), 환불 완료 전
            refundAccountEditable = v2UserOrderDomainService.canEditRefundAccount(order),
            refundAccountRequired = v2UserOrderDomainService.isRefundAccountRequired(order, refundAccount),
            issuedTickets = tickets.map { t ->
                val giftState = giftStates.getValue(t.id)
                V2MyOrderIssuedTicketResponse(
                    ticketUuid = t.uuid.takeIf { giftState != V2GiftState.SENT },
                    issuedTicketNo = t.issuedTicketNo,
                    ticketName = t.itemInfo?.ticketName,
                    entrance = V2EntranceState.of(t.issuedTicketStatus),
                    enteredAt = t.enteredAt,
                    optionAnswers = mapper.toOptionAnswers(ticketAnswers.getValue(t.id), names),
                    giftState = giftState,
                    isGiftExpired = giftState == V2GiftState.PENDING && giftDomainService.isEventEnded(event, now),
                    giftId = latestGifts[t.id]?.id?.takeIf { giftState == V2GiftState.PENDING || giftState == V2GiftState.SENT },
                )
            },
            canCancel = v2UserOrderDomainService.canCancel(order, event, now),
        )
    }

    private fun eventsOf(eventIds: Collection<Long>): Map<Long?, Event> =
        if (eventIds.isEmpty()) emptyMap() else eventAdaptor.findAllByIds(eventIds.distinct()).associateBy { it.id }

    private fun eventResponse(event: Event, now: LocalDateTime) = V2MyOrderEventResponse(
        eventId = event.id!!,
        name = event.eventBasic?.name,
        posterImageUrl = event.eventDetail?.posterImage?.generateImageUrl(),
        startAt = event.getStartAt(),
        displayStatus = V2EventDisplayRule.of(event, now),
    )
}
