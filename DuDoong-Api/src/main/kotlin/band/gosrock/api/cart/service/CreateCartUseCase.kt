package band.gosrock.api.cart.service

import band.gosrock.api.cart.model.dto.request.AddCartRequest
import band.gosrock.api.cart.model.dto.response.CartResponse
import band.gosrock.api.cart.model.mapper.CartMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.cart.service.CartDomainService
import org.springframework.transaction.annotation.Transactional

@UseCase
class CreateCartUseCase(
    private val cartDomainService: CartDomainService,
    private val cartMapper: CartMapper,
) {
    @Transactional
    fun execute(userId: Long, addCartRequest: AddCartRequest): CartResponse {
        val cart = cartMapper.toEntity(addCartRequest, userId)
        val cartId = cartDomainService.createCart(cart, userId)
        return cartMapper.toCartResponse(cartId)
    }
}
