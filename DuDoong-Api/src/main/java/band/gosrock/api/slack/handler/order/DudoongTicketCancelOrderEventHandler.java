package band.gosrock.api.slack.handler.order;


import band.gosrock.domain.common.alarm.HostSlackAlarm;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
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
public class DudoongTicketCancelOrderEventHandler {
    private final EventAdaptor eventAdaptor;

    private final HostAdaptor hostAdaptor;
    private final OrderAdaptor orderAdaptor;
    private final SlackMessageProvider slackMessageProvider;

    @Async
    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(readOnly = true)
    public void handle(WithDrawOrderEvent withDrawOrderEvent) {
        log.info("두둥 티켓 취소 전송되는 알림");
        if (!withDrawOrderEvent.getIsDudoongTicketOrder()) return;
        if (withDrawOrderEvent.getIsRefund()) return;
        Order order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.getOrderUuid());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        String message = HostSlackAlarm.dudoongOrderCancel(event, order);
        slackMessageProvider.sendMessage(host.getSlackUrl(), message);
    }
}
