package band.gosrock.domain.domains.order.service.handler;


import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import band.gosrock.domain.domains.order.service.WithdrawPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithDrawOrderHandler {

    private final WithdrawPaymentService withdrawPaymentService;

    private final OrderAdaptor orderAdaptor;

    @Async
    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void handleRegisterUserEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getOrderUuid() + "주문 철회 핸들러");
        OrderStatus orderStatus = withDrawOrderEvent.getOrderStatus();
        Order order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.getOrderUuid());
        String reason = "결제 취소";
        if (orderStatus.equals(OrderStatus.CANCELED)) {
            reason = "이벤트 관리자에 의한 취소";

        } else if (orderStatus.equals(OrderStatus.REFUND)) {
            reason = "구매자에의한 환불 요청";
        }

        log.info(withDrawOrderEvent.getOrderUuid() + "주문 철회" + order.getPaymentKey() + reason);
        withdrawPaymentService.execute(order.getUuid(), order.getPaymentKey(), reason);
    }
}
