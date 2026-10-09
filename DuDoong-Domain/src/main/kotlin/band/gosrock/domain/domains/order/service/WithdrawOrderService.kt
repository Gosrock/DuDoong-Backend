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

    /** 호스트 취소. 락 안에서 읽은 주문이 경로의 공연 소속이 아니면 404 (#760) */
    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun cancelOrder(eventId: Long, orderUuid: String, reason: String? = null): String {
        val order = orderAdaptor.findEventOrder(eventId, orderUuid)
        order.cancel(orderValidator, reason)
        return orderUuid
    }

    /** 운영 어드민 취소. 공연 경로가 없는 호출부 전용이라 공연 소속을 검사하지 않는다 (권한 검사는 호출 측) */
    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun cancelOrderByAdmin(orderUuid: String, reason: String? = null): String {
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

    /** 호스트 거절. 락 안에서 읽은 주문이 경로의 공연 소속이 아니면 404 (#760) */
    @JvmOverloads
    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    fun refuseOrder(eventId: Long, orderUuid: String, reason: String? = null): String {
        val order = orderAdaptor.findEventOrder(eventId, orderUuid)
        order.refuse(orderValidator, reason)
        return orderUuid
    }
}
