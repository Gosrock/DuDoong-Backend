package band.gosrock.domain.domains.cart.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.domain.domains.cart.domain.Cart
import band.gosrock.domain.domains.cart.exception.CartNotFoundException
import band.gosrock.domain.domains.cart.repository.CartRepository
import java.util.Optional

@Adaptor
class CartAdaptor(private val cartRepository: CartRepository) {

    fun save(cart: Cart): Cart = cartRepository.save(cart)

    fun queryCart(cartId: Long): Cart =
        cartRepository.findById(cartId).orElseThrow { CartNotFoundException.EXCEPTION }

    fun queryCart(cartId: Long, userId: Long): Cart =
        cartRepository.findByIdAndUserId(cartId, userId)
            .orElseThrow { CartNotFoundException.EXCEPTION }

    fun findCartByUserId(userId: Long): Optional<Cart> =
        cartRepository.findByUserId(userId)

    fun find(cartId: Long): Cart =
        cartRepository.find(cartId).orElseThrow { CartNotFoundException.EXCEPTION }

    fun deleteByUserId(userId: Long) {
        cartRepository.deleteByUserId(userId)
    }
}
