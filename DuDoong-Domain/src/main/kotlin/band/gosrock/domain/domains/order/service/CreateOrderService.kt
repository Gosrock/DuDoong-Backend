package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class CreateOrderService(
    private val orderFactory: OrderFactory,
    private val orderAdaptor: OrderAdaptor,
) {
    @RedissonLock(LockName = "주문생성", identifier = "userId")
    fun withOutCoupon(cartId: Long, userId: Long): String {
        val order = orderFactory.createNormalOrder(cartId, userId)
        return orderAdaptor.save(order).uuid!!
    }

    @RedissonLock(LockName = "주문생성", identifier = "userId")
    fun withCoupon(cartId: Long, userId: Long, couponId: Long): String {
        val order = orderFactory.createCouponOrder(cartId, userId, couponId)
        return orderAdaptor.save(order).uuid!!
    }
}
