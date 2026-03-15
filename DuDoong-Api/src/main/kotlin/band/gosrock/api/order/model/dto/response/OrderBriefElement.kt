package band.gosrock.api.order.model.dto.response

import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.common.vo.RefundInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTickets
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketsStage
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import io.swagger.v3.oas.annotations.media.Schema

data class OrderBriefElement(
    @Schema(description = "예매 취소 정보")
    val refundInfo: RefundInfoVo,

    @Schema(description = "이벤트 프로필 정보")
    val eventProfile: EventProfileVo,

    @Schema(description = "주문 고유 uuid")
    val orderUuid: String,

    @Schema(description = "주문 번호 R------- 형식")
    val orderNo: String,

    @Schema(description = "주문에 딸린 티켓들의 상태")
    val stage: IssuedTicketsStage,

    @Schema(description = "주문의 상태")
    val orderStatus: OrderStatus,

    @Schema(description = "아이템 이름")
    val itemName: String,

    @Schema(description = "아이템 총 갯수")
    val totalQuantity: Long,
) {
    companion object {
        @JvmStatic
        fun of(order: Order, event: Event, issuedTickets: IssuedTickets): OrderBriefElement =
            OrderBriefElement(
                refundInfo = event.toRefundInfoVoWithOrderStatus(order.orderStatus),
                stage = issuedTickets.getIssuedTicketsStage(event),
                orderUuid = order.uuid!!,
                orderNo = order.orderNo!!,
                orderStatus = order.orderStatus,
                eventProfile = event.toEventProfileVo(),
                itemName = order.orderName!!,
                totalQuantity = order.getTotalQuantity(),
            )
    }
}
