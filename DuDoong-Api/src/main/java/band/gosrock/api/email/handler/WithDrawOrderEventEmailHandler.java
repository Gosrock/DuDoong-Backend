package band.gosrock.api.email.handler;


import band.gosrock.api.email.dto.OrderMailDto;
import band.gosrock.api.email.service.OrderMailInfoHelper;
import band.gosrock.api.email.service.OrderWithDrawCancelEmailService;
import band.gosrock.api.email.service.OrderWithDrawRefundEmailService;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithDrawOrderEventEmailHandler {

    private final OrderWithDrawRefundEmailService orderWithDrawRefundEmailService;
    private final OrderWithDrawCancelEmailService orderWithDrawCancelEmailService;
    private final OrderMailInfoHelper orderMailInfoHelper;

    @Async
    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithDrawOrderEvent(WithDrawOrderEvent withDrawOrderEvent) {
        OrderStatus orderStatus = withDrawOrderEvent.getOrderStatus();
        OrderMailDto orderMailDto = orderMailInfoHelper.execute(withDrawOrderEvent.getOrderUuid());

        // 관리자에 의한 취소
        if (orderStatus == OrderStatus.CANCELED) {
            orderWithDrawCancelEmailService.execute(orderMailDto);
            return;
        }
        // 구매자에의한 환불 요청
        orderWithDrawRefundEmailService.execute(orderMailDto);
    }
}
