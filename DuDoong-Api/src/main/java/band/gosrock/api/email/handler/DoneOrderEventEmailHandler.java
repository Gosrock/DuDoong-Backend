package band.gosrock.api.email.handler;


import band.gosrock.api.email.dto.OrderMailDto;
import band.gosrock.api.email.service.OrderApproveConfirmEmailService;
import band.gosrock.api.email.service.OrderMailInfoHelper;
import band.gosrock.api.email.service.OrderPaymentDoneEmailService;
import band.gosrock.domain.common.events.order.DoneOrderEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoneOrderEventEmailHandler {

    private final OrderMailInfoHelper orderMailInfoHelper;

    private final OrderApproveConfirmEmailService orderApproveConfirmEmailService;
    private final OrderPaymentDoneEmailService orderPaymentDoneEmailService;

    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 이메일 보내기");
        OrderMailDto orderMailDto = orderMailInfoHelper.execute(doneOrderEvent.getOrderUuid());

        // 결제용
        if (doneOrderEvent.getOrderMethod().isPayment()) {
            orderPaymentDoneEmailService.execute(orderMailDto);
            return;
        }
        // 승인용
        orderApproveConfirmEmailService.execute(orderMailDto);
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 이메일 보내기 완료");
    }
}
