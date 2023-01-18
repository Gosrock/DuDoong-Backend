package band.gosrock.domain.domains.cart.service;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoneOrderEventHandler {

    private final CartAdaptor cartAdaptor;

    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getUuid() + "주문 상태 완료, 장바구니를 제거합니다.");
        Long userId = doneOrderEvent.getOrder().getUserId();
        cartAdaptor.deleteByUserId(userId);
    }
}
