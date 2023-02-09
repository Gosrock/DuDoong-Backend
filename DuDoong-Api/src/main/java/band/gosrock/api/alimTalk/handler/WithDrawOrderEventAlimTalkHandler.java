package band.gosrock.api.alimTalk.handler;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.api.alimTalk.service.OrderAlimTalkInfoHelper;
import band.gosrock.api.alimTalk.service.SendWithdrawOrderAlimTalkService;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithDrawOrderEventAlimTalkHandler {
    private final SendWithdrawOrderAlimTalkService sendWithdrawOrderAlimTalkService;
    private final OrderAlimTalkInfoHelper orderAlimTalkInfoHelper;
    private final OrderAdaptor orderAdaptor;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    @Async
    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleWithDrawOrderEvent(WithDrawOrderEvent withDrawOrderEvent) {
        OrderAlimTalkDto orderAlimTalkDto =
                orderAlimTalkInfoHelper.execute(withDrawOrderEvent.getOrderUuid());

        // 파트너인 호스트의 공연일 경우만 알림톡 전송
        Order order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.getOrderUuid());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        if (host.isPartnerHost()) {
            sendWithdrawOrderAlimTalkService.execute(orderAlimTalkDto);
        }
    }
}
