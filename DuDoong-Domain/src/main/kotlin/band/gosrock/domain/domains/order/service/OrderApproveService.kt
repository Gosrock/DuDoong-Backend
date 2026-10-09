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
    /** 공연 소속 검증은 락 안에서 읽은 주문으로 한다: 다른 공연의 주문이면 404 (#760) */
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun execute(eventId: Long, orderUuid: String): String {
        val order = orderAdaptor.findEventOrder(eventId, orderUuid)
        order.approve(orderValidator)
        return orderUuid
    }
}
