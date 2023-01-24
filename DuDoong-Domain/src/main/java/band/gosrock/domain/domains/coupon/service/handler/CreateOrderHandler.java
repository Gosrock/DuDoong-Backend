package band.gosrock.domain.domains.coupon.service.handler;


import band.gosrock.domain.common.events.order.CreateOrderEvent;
import band.gosrock.domain.domains.coupon.service.UseCouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderHandler {

    private final UseCouponService useCouponService;

    @TransactionalEventListener(
            classes = CreateOrderEvent.class,
            phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDoneOrderFailEvent(CreateOrderEvent createOrderEvent) {
        log.info(createOrderEvent.getOrderUuid() + "주문 생성 이벤트 쿠폰 사용 리스너");

        if (createOrderEvent.getIsUsingCoupon()) {
            log.info(createOrderEvent.getOrderUuid() + "주문 생성 이벤트 쿠폰 사용 리스너 : 쿠폰 사용 도메인 서비스 호출");
            useCouponService.execute(
                    createOrderEvent.getUserId(), createOrderEvent.getIssuedCouponId());
            log.info(createOrderEvent.getOrderUuid() + "주문 생성 이벤트 쿠폰 사용 리스너 : 쿠폰 사용 도메인 서비스 호출종료");
        }
    }
}
