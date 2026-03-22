package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import org.slf4j.LoggerFactory
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class WithdrawOrderService(
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun cancelOrder(orderUuid: String, reason: String? = null): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.cancel(orderValidator, reason)
        return orderUuid
    }

    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun refundOrder(orderUuid: String, userId: Long, reason: String? = null): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.refund(userId, orderValidator, reason)
        return orderUuid
    }

    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun refuseOrder(orderUuid: String, reason: String? = null): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.refuse(orderValidator, reason)
        return orderUuid
    }
}
