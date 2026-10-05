package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.cart.repository.CartLockKeyQuery
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * v1 주문 생성. 락은 v2 주문 생성(`V2UserOrderDomainService.create`)·발급·티켓 수정과 같은 `티켓관리:{ticketItemId}` (#724).
 * 예전에는 `주문생성:{userId}` 락이라 v1·v2 동시 주문, 다른 사용자 동시 주문에서 승인 대기 재고·1인 제한 검사가 줄 서지 않았다.
 * 사용자 락은 두지 않는다: v1 장바구니는 사용자당 1개(같은 장바구니 = 같은 티켓)라 사용자별로 지켜야 할 것이 티켓 락 안에 모두 들어 있다.
 * 락 순서: 티켓관리 → (주문 생성 이벤트 BEFORE_COMMIT) 쿠폰. 티켓관리를 쥔 채 다른 락을 기다리는 경로는 이것뿐이고, 쿠폰 락 안에서 티켓관리를 기다리는 경로는 없다
 */
@DomainService
@Transactional(readOnly = true)
class CreateOrderService(
    private val orderFactory: OrderFactory,
    private val orderAdaptor: OrderAdaptor,
    private val cartLockKeyQuery: CartLockKeyQuery,
) {
    /** 락 키(장바구니의 티켓 id). 락 전에 JDBC 로 읽어 커넥션을 바로 돌려준다 ([CartLockKeyQuery]). 예외는 예전과 같다 (Cart_404_1 등) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun itemIdOfCart(cartId: Long, userId: Long): Long = cartLockKeyQuery.itemIdOf(cartId, userId)

    @RedissonLock(LockName = TICKET_LOCK, identifier = "itemId")
    fun withOutCoupon(itemId: Long, cartId: Long, userId: Long): String {
        val order = orderFactory.createNormalOrder(cartId, userId)
        return orderAdaptor.save(order).uuid!!
    }

    @RedissonLock(LockName = TICKET_LOCK, identifier = "itemId")
    fun withCoupon(itemId: Long, cartId: Long, userId: Long, couponId: Long): String {
        val order = orderFactory.createCouponOrder(cartId, userId, couponId)
        return orderAdaptor.save(order).uuid!!
    }

    companion object {
        private const val TICKET_LOCK = "티켓관리"
    }
}
