package band.gosrock.api.order.model.dto.response

import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.common.vo.IssuedTicketInfoVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.order.domain.Order
import io.swagger.v3.oas.annotations.media.Schema

data class OrderTicketResponse(
    @Schema(description = "예매 정보( 티켓 목록 )")
    val tickets: List<IssuedTicketInfoVo>,

    @Schema(description = "이벤트 프로필 정보")
    val eventProfile: EventProfileVo,

    @Schema(description = "주문 고유 uuid")
    val orderUuid: String,

    @Schema(description = "주문 번호 R------- 형식")
    val orderNo: String,
) {
    companion object {
        @JvmStatic
        fun of(order: Order, event: Event, tickets: List<IssuedTicketInfoVo>): OrderTicketResponse =
            OrderTicketResponse(
                tickets = tickets,
                orderUuid = order.uuid!!,
                orderNo = order.orderNo!!,
                eventProfile = event.toEventProfileVo(),
            )
    }
}
