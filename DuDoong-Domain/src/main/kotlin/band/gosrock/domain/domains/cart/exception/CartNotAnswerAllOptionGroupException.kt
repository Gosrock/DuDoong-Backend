package band.gosrock.domain.domains.cart.exception

import band.gosrock.common.exception.DuDoongCodeException

class CartNotAnswerAllOptionGroupException private constructor() : DuDoongCodeException(CartErrorCode.CART_NOT_ALL_ANSWER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CartNotAnswerAllOptionGroupException()
    }
}
