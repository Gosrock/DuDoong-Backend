package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.exception.DuDoongCodeException

class CartNotFoundException private constructor() : DuDoongCodeException(CartErrorCode.CART_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CartNotFoundException()
    }
}
