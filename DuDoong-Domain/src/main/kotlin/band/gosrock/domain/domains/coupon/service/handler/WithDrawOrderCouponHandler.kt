package band.gosrock.domain.domains.coupon.service.handler

import band.gosrock.domain.common.events.order.WithDrawOrderEvent
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class WithDrawOrderCouponHandler(
    private val recoveryCouponService: RecoveryCouponService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(classes = [WithDrawOrderEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
    fun handleWithDrawOrderEvent(withDrawOrderEvent: WithDrawOrderEvent) {
        log.info("${withDrawOrderEvent.orderUuid} 주문 철회 이벤트 쿠폰 회복 리스너")
        if (withDrawOrderEvent.isUsingCoupon) {
            log.info("${withDrawOrderEvent.orderUuid} 주문 철회 이벤트 쿠폰 회복 리스너 : 쿠폰 회복 도메인 서비스 호출")
            recoveryCouponService.execute(withDrawOrderEvent.userId, withDrawOrderEvent.issuedCouponId!!)
            log.info("${withDrawOrderEvent.orderUuid} 주문 철회 이벤트 쿠폰 사용 리스너 : 쿠폰 회복 도메인 서비스 호출 종료")
        }
    }
}
