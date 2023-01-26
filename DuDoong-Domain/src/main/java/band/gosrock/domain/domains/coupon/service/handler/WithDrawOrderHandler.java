package band.gosrock.domain.domains.coupon.service.handler;


import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithDrawOrderHandler {
    private final RecoveryCouponService recoveryCouponService;

    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.BEFORE_COMMIT)
    public void handleWithDrawOrderEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getOrderUuid() + "주문 철회 이벤트 쿠폰 회복 리스너");
        if (withDrawOrderEvent.getIsUsingCoupon()) {
            log.info(withDrawOrderEvent.getOrderUuid() + "주문 철회 이벤트 쿠폰 회복 리스너 : 쿠폰 회복 도메인 서비스 호출");
            recoveryCouponService.execute(
                    withDrawOrderEvent.getUserId(), withDrawOrderEvent.getIssuedCouponId());
            log.info(
                    withDrawOrderEvent.getOrderUuid()
                            + "주문 철회 이벤트 쿠폰 사용 리스너 : 쿠폰 회복 도메인 서비스 호출 종료");
        }
    }
}
