package band.gosrock.domain.domains.coupon.service.handler

import band.gosrock.domain.common.events.order.CreateOrderEvent
import band.gosrock.domain.domains.coupon.service.UseCouponService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class CreateOrderCouponHandler(
    private val useCouponService: UseCouponService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener(classes = [CreateOrderEvent::class], phase = TransactionPhase.BEFORE_COMMIT)
    fun handleDoneOrderFailEvent(createOrderEvent: CreateOrderEvent) {
        log.info("${createOrderEvent.orderUuid} 주문 생성 이벤트 쿠폰 사용 리스너")
        if (createOrderEvent.isUsingCoupon) {
            log.info("${createOrderEvent.orderUuid} 주문 생성 이벤트 쿠폰 사용 리스너 : 쿠폰 사용 도메인 서비스 호출")
            useCouponService.execute(createOrderEvent.userId, createOrderEvent.issuedCouponId!!)
            log.info("${createOrderEvent.orderUuid} 주문 생성 이벤트 쿠폰 사용 리스너 : 쿠폰 사용 도메인 서비스 호출 종료")
        }
    }
}
