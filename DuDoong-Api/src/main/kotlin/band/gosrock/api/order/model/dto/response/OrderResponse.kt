package band.gosrock.api.order.model.dto.response

import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.common.vo.RefundInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import io.swagger.v3.oas.annotations.media.Schema

data class OrderResponse(
    @Schema(description = "결제 정보")
    val paymentInfo: OrderPaymentResponse,

    @Schema(description = "예매 정보( 티켓 목록 )")
    val tickets: List<OrderLineTicketResponse>,

    @Schema(description = "예매 취소 정보")
    val refundInfo: RefundInfoVo,

    @Schema(description = "이벤트 프로필 정보")
    val eventProfile: EventProfileVo,

    @Schema(description = "주문 고유 uuid")
    val orderUuid: String,

    @Schema(description = "주문 번호 R------- 형식")
    val orderNo: String,

    @Schema(description = "주문 방식 ( 결제 방식 , 승인 방식 )")
    val orderMethod: OrderMethod,
) {
    companion object {
        @JvmStatic
        fun of(order: Order, event: Event, tickets: List<OrderLineTicketResponse>): OrderResponse =
            OrderResponse(
                refundInfo = event.toRefundInfoVoWithOrderStatus(order.orderStatus),
                orderMethod = order.orderMethod!!,
                paymentInfo = OrderPaymentResponse.from(order),
                tickets = tickets,
                orderUuid = order.uuid!!,
                orderNo = order.orderNo!!,
                eventProfile = event.toEventProfileVo(),
            )
    }
}
