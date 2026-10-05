package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.common.aop.redissonLock.LockNames
import band.gosrock.domain.common.aop.redissonLock.RedissonLock
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import band.gosrock.domain.domains.cart.repository.CartLockKey
import band.gosrock.domain.domains.cart.repository.CartLockKeyQuery
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.exception.InvalidOrderException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * v1 주문 생성 (#724). 락은 주문 종류로 나눈다:
 * - **승인형**(두둥티켓, 무료 승인형): `티켓관리:{ticketItemId}` — v2 주문 생성·발급·티켓 수정과 같은 락. v1·v2 동시 주문, 다른 사용자 동시 주문에서
 *   승인 대기 재고·1인 제한 검사가 줄 선다 (예전 `주문생성:{userId}` 로는 줄 서지 않았다 — E2E test_53). 사용자 락은 두지 않는다: v1 장바구니는 사용자당 1개(= 같은 티켓)
 * - **결제형·선착순·쿠폰**: 예전 그대로 `주문생성:{userId}`. 인기 티켓 오픈 때 생성이 한 줄로 서고, 같은 티켓 락을 쓰는 승인 발급까지 락 대기 시간 초과로 실패하지 않게 한다
 *
 * 락 순서: 티켓관리 → (주문 생성 이벤트 BEFORE_COMMIT) 쿠폰. 티켓관리를 쥔 채 다른 락을 기다리는 경로는 이것뿐이고, 쿠폰 락 안에서 티켓관리를 기다리는 경로는 없다
 */
@DomainService
@Transactional(readOnly = true)
class CreateOrderService(
    private val orderFactory: OrderFactory,
    private val orderAdaptor: OrderAdaptor,
    private val cartAdaptor: CartAdaptor,
    private val cartLockKeyQuery: CartLockKeyQuery,
) {
    /** 락 키(장바구니의 티켓 id·승인형 여부). 락 전에 JDBC 로 읽어 커넥션을 바로 돌려준다 ([CartLockKeyQuery]). 예외는 예전과 같다 (Cart_404_1 등) */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun lockKeyOf(cartId: Long, userId: Long): CartLockKey = cartLockKeyQuery.lockKeyOf(cartId, userId)

    /** 승인형 주문 생성 (티켓 락). 락 안에서 장바구니의 티켓이 락 키와 같은지 다시 확인한다 (장바구니 줄은 바뀌지 않으므로 방어) */
    @RedissonLock(LockName = LockNames.TICKET, identifier = "itemId")
    fun withOutCouponApproval(itemId: Long, cartId: Long, userId: Long): String {
        val cart = cartAdaptor.queryCart(cartId, userId)
        if (cart.getItemId() != itemId) throw InvalidOrderException.EXCEPTION
        val order = orderFactory.createNormalOrder(cart, userId)
        return orderAdaptor.save(order).uuid!!
    }

    /** 결제형·선착순 주문 생성 (사용자 락, 예전 그대로) */
    @RedissonLock(LockName = LockNames.ORDER_CREATE, identifier = "userId")
    fun withOutCoupon(cartId: Long, userId: Long): String {
        val order = orderFactory.createNormalOrder(cartId, userId)
        return orderAdaptor.save(order).uuid!!
    }

    /** 쿠폰 주문 생성 (결제형 선착순만 가능 — 사용자 락, 예전 그대로) */
    @RedissonLock(LockName = LockNames.ORDER_CREATE, identifier = "userId")
    fun withCoupon(cartId: Long, userId: Long, couponId: Long): String {
        val order = orderFactory.createCouponOrder(cartId, userId, couponId)
        return orderAdaptor.save(order).uuid!!
    }
}
