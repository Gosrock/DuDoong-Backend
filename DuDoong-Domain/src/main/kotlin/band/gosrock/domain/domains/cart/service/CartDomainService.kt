package band.gosrock.domain.domains.cart.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import band.gosrock.domain.domains.cart.domain.Cart

@DomainService
class CartDomainService(private val cartAdaptor: CartAdaptor) {

    @RedissonLock(LockName = "카트생성", paramClassType = Cart::class, identifier = "userId", needSameTransaction = true)
    fun createCart(cart: Cart, userId: Long): Long {
        cartAdaptor.deleteByUserId(userId)
        val savedCart = cartAdaptor.save(cart)
        return savedCart.id!!
    }
}
