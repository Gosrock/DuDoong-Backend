package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class OrderApproveService(
    private val orderAdaptor: OrderAdaptor,
    private val orderValidator: OrderValidator,
) {
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun execute(orderUuid: String): String {
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        order.approve(orderValidator)
        return orderUuid
    }
}
