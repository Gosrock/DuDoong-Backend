package band.gosrock.api.cart.model.dto.response

import band.gosrock.domain.common.vo.AccountInfoVo
import band.gosrock.domain.common.vo.EventProfileVo
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import io.swagger.v3.oas.annotations.media.Schema

data class CartResponse(
    @Schema(description = "장바구니명 입니다.", defaultValue = "")
    val title: String,

    val items: List<CartItemResponse>,

    @Schema(description = "카트라인들의 총 결제금액을 합한 금액입니다", defaultValue = "15000원")
    val totalPrice: Money,

    @Schema(description = "생성한 장바구니의 아이디입니다", defaultValue = "30")
    val cartId: Long,

    @Schema(description = "전체 아이템 수량을 의미합니다", defaultValue = "3")
    val totalQuantity: Long,

    @Schema(
        description = "결제가 필요한지에 대한 여부를 결정합니다. 필요한 true면 결제창 띄우시면됩니다. 이단계에선 무시하셔도 됩니다.",
        defaultValue = "true",
    )
    val isNeedPayment: Boolean,

    @Schema(description = "티켓의 타입. 승인 , 선착순 두가지입니다.")
    val approveType: TicketType,

    @Schema(description = "티켓의 지불 타입. 두둥티켓, 무료 , 유료 세가지입니다.")
    val ticketPayType: TicketPayType,

    @Schema(description = "계좌정보", nullable = true)
    val accountInfo: AccountInfoVo?,

    @Schema(description = "이벤트 정보")
    val eventProfile: EventProfileVo,
) {
    companion object {
        @JvmStatic
        fun of(cartItemResponses: List<CartItemResponse>, cart: Cart, item: TicketItem, event: Event): CartResponse {
            return CartResponse(
                items = cartItemResponses,
                totalPrice = cart.getTotalPrice(),
                cartId = cart.id!!,
                title = cart.cartName!!,
                isNeedPayment = cart.isNeedPaid(),
                totalQuantity = cart.getTotalQuantity(),
                approveType = item.type!!,
                ticketPayType = item.payType!!,
                accountInfo = item.accountInfo,
                eventProfile = event.toEventProfileVo(),
            )
        }
    }
}
