package band.gosrock.api.eventHandlers;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.common.events.user.UserRegisterEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getUuid() + "주문 상태 완료, 티켓 생성필요");
    }

    @Async
    @TransactionalEventListener(
        classes = WithDrawOrderEvent.class,
        phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegisterUserEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getUuid() + "주문 상태 철회 , 티켓 제거 필요");
    }
}
