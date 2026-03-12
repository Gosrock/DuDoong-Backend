package band.gosrock.domain.domains.cart.repository

import band.gosrock.domain.domains.cart.domain.Cart
import java.util.Optional

interface CartCustomRepository {
    fun find(cartId: Long): Optional<Cart>
}
