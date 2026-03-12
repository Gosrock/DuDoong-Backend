package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.exception.DuDoongCodeException

class CartItemNotOneTypeException private constructor() : DuDoongCodeException(CartErrorCode.CART_INVALID_ITEM_KIND_POLICY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CartItemNotOneTypeException()
    }
}
