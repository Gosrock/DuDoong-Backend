package band.gosrock.api.order.model.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class OrderLineTicketResponse(
    @Schema(description = "티켓명", defaultValue = "일반티켓")
    val ticketName: String,

    @Schema(description = "예매 번호", defaultValue = "R1000001-123")
    val orderNo: String,

    @Schema(description = "티켓 번호", defaultValue = "T1000001 ~ T1000002 (2매)")
    val ticketNos: String,

    @Schema(description = "구매 일시")
    @DateFormat
    val paymentAt: LocalDateTime?,

    @Schema(description = "유저이름")
    val userName: String,

    @Schema(description = "금액")
    val orderLinePrice: Money,

    @Schema(description = "구매수량")
    val purchaseQuantity: Long,

    @Schema(description = "옵션의 응답 목록")
    val answers: List<OptionAnswerVo>,

    @Schema(description = "각 옵션 가격")
    val eachOptionPrice: Money,
) {
    companion object {
        @JvmStatic
        fun of(
            order: Order,
            orderLineItem: OrderLineItem,
            optionAnswerVos: List<OptionAnswerVo>,
            userName: String,
            ticketNos: String,
        ): OrderLineTicketResponse =
            OrderLineTicketResponse(
                answers = optionAnswerVos,
                orderNo = order.orderNo + "-" + orderLineItem.id,
                ticketNos = ticketNos,
                ticketName = orderLineItem.getItemName(),
                paymentAt = order.approvedAt,
                userName = userName,
                orderLinePrice = orderLineItem.getTotalOrderLinePrice(),
                purchaseQuantity = orderLineItem.quantity!!,
                eachOptionPrice = orderLineItem.getOptionAnswersPrice(),
            )
    }
}
