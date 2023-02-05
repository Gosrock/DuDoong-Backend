package band.gosrock.api.email.handler;


import band.gosrock.api.email.service.OrderApproveRequestEmailService;
import band.gosrock.api.email.service.OrderMailInfoHelper;
import band.gosrock.domain.common.events.order.CreateOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateOrderEventEmailHandler {

    private final OrderApproveRequestEmailService orderApproveRequestEmailService;

    private final OrderMailInfoHelper orderMailInfoHelper;

    @Async
    @TransactionalEventListener(
            classes = CreateOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderFailEvent(CreateOrderEvent createOrderEvent) {
        log.info(createOrderEvent.getOrderUuid() + "주문 생성 이메일 요청");

        // 승인 주문만 생성시에 이메일을 보낸다.
        if (createOrderEvent.getOrderMethod().isPayment()) {
            return;
        }

        orderApproveRequestEmailService.execute(
                orderMailInfoHelper.execute(createOrderEvent.getOrderUuid()));
    }
}
