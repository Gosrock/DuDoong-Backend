package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.exception.DuDoongCodeException

class CartInvalidOptionAnswerException private constructor() : DuDoongCodeException(CartErrorCode.CART_INVALID_OPTION_ANSWER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CartInvalidOptionAnswerException()
    }
}
