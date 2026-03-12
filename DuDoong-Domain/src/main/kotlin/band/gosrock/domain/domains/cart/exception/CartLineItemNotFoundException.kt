package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.exception.DuDoongCodeException

class CartLineItemNotFoundException private constructor() : DuDoongCodeException(CartErrorCode.CART_LINE_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CartLineItemNotFoundException()
    }
}
