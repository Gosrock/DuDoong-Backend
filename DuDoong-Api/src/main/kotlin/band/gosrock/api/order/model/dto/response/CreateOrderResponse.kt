package band.gosrock.api.order.model.dto.response

import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.user.domain.Profile
import io.swagger.v3.oas.annotations.media.Schema

data class CreateOrderResponse(
    @Schema(description = "UUId")
    val orderId: String,

    @Schema(description = "상품명")
    val orderName: String,

    @Schema(description = "고객이메일")
    val customerEmail: String,

    @Schema(description = "고객이름")
    val customerName: String,

    @Schema(description = "결제금액")
    val amount: Money,

    @Schema(description = "결제가 필요한지에 대한 여부를 결정합니다. 필요한 true면 결제창 띄우시면됩니다.", defaultValue = "true")
    val isNeedPayment: Boolean,

    @Schema(description = "주문 방식 ( 결제 방식 , 승인 방식 )")
    val orderMethod: OrderMethod,

    @Schema(description = "티켓의 타입. 승인 , 선착순 두가지입니다.")
    val approveType: TicketType,

    @Schema(description = "티켓의 지불 타입. 두둥티켓, 무료 , 유료 세가지입니다.")
    val ticketPayType: TicketPayType,

    @Schema(description = "계좌정보", nullable = true)
    val accountInfo: AccountInfoVo?,
) {
    companion object {
        @JvmStatic
        fun from(order: Order, item: TicketItem, profile: Profile): CreateOrderResponse =
            CreateOrderResponse(
                customerEmail = profile.email!!,
                customerName = profile.name!!,
                orderName = order.orderName!!,
                orderId = order.uuid!!,
                amount = order.getTotalPaymentPrice(),
                orderMethod = order.orderMethod!!,
                isNeedPayment = order.isNeedPaid(),
                approveType = item.type!!,
                ticketPayType = item.payType!!,
                accountInfo = item.accountInfo,
            )
    }
}
