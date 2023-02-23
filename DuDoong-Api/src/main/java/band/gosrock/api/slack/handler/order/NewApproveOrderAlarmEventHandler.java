package band.gosrock.api.slack.handler.order;


import band.gosrock.domain.common.alarm.HostSlackAlarm;
import band.gosrock.domain.common.events.order.CreateOrderEvent;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.infrastructure.config.slack.SlackMessageProvider;
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
public class NewApproveOrderAlarmEventHandler {
    private final EventAdaptor eventAdaptor;

    private final HostAdaptor hostAdaptor;
    private final OrderAdaptor orderAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = CreateOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void handle(CreateOrderEvent createOrderEvent) {
        // 승인 방식의 결제만 알림 발송 대상.
        if (createOrderEvent.getOrderMethod().isPayment()) return;
        log.info("승인 방식 요청 알림 전송");
        Order order = orderAdaptor.findByOrderUuid(createOrderEvent.getOrderUuid());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        String message = HostSlackAlarm.newApproveOrder(event, order);
        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
