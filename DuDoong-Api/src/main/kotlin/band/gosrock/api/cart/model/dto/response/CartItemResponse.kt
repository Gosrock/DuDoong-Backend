package band.gosrock.api.cart.model.dto.response

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.common.vo.OptionAnswerVo
import band.gosrock.domain.domains.cart.domain.CartLineItem
import io.swagger.v3.oas.annotations.media.Schema

data class CartItemResponse(
    @Schema(description = "카트라인의 이름입니다.", defaultValue = "일반티켓 2매")
    val name: String,

    val answers: List<OptionAnswerVo>,

    @Schema(description = "아이템 공급가액입니다.", defaultValue = "3000원")
    val itemPrice: Money,

    @Schema(
        description = "카트 라인의 총 가격입니다. 옵션등을 통해서 공급가액에 합산되는 형식입니다. (아이템가격 + 옵션가) * 아이템 개수",
        defaultValue = "4000원",
    )
    val cartLinePrice: Money,

    @Schema(description = "담은 상품의 개수입니다.", defaultValue = "1")
    val packedQuantity: Long,

    @Schema(description = "각 옵션 가격")
    val eachOptionPrice: Money,
) {
    companion object {
        @JvmStatic
        fun of(cartLineItem: CartLineItem, itemName: String, answers: List<OptionAnswerVo>): CartItemResponse {
            return CartItemResponse(
                answers = answers,
                name = itemName,
                cartLinePrice = cartLineItem.getTotalCartLinePrice(),
                itemPrice = cartLineItem.itemPrice!!,
                packedQuantity = cartLineItem.quantity!!,
                eachOptionPrice = cartLineItem.getTotalOptionsPrice(),
            )
        }
    }
}
