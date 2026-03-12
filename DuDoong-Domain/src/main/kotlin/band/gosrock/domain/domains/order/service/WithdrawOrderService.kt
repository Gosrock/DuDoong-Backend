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

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun cancelOrder(orderUuid: String): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.cancel(orderValidator)
        return orderUuid
    }

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun refundOrder(orderUuid: String, userId: Long): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.refund(userId, orderValidator)
        return orderUuid
    }

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun refuseOrder(orderUuid: String): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.refuse(orderValidator)
        return orderUuid
    }
}
