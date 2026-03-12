package band.gosrock.domain.domains.cart.repository

import band.gosrock.domain.domains.cart.domain.Cart
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CartRepository : JpaRepository<Cart, Long>, CartCustomRepository {
    fun findByIdAndUserId(id: Long, userId: Long): Optional<Cart>
    fun findByUserId(userId: Long): Optional<Cart>
    fun deleteByUserId(userId: Long): Optional<Cart>
}
