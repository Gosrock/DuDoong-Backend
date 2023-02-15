package band.gosrock.api.alimTalk.handler;


import band.gosrock.api.alimTalk.dto.OrderAlimTalkDto;
import band.gosrock.api.alimTalk.service.SendDoneOrderAlimTalkService;
import band.gosrock.api.alimTalk.service.helper.OrderAlimTalkInfoHelper;
import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import com.google.i18n.phonenumbers.NumberParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoneOrderEventAlimTalkHandler {

    private final SendDoneOrderAlimTalkService sendDoneOrderAlimTalkService;
    private final OrderAlimTalkInfoHelper orderAlimTalkInfoHelper;
    private final OrderAdaptor orderAdaptor;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;

    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) throws NumberParseException {
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 파트너의 공연이면 알림톡 전송");
        OrderAlimTalkDto orderAlimTalkDto =
                orderAlimTalkInfoHelper.execute(doneOrderEvent.getOrderUuid());
        // 파트너인 호스트의 공연일 경우만 알림톡 전송
        Order order = orderAdaptor.findByOrderUuid(doneOrderEvent.getOrderUuid());
        Event event = eventAdaptor.findById(order.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        if (host.isPartnerHost()) {
            sendDoneOrderAlimTalkService.execute(orderAlimTalkDto);
            log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 파트너의 공연 알림톡 전송 완료");
        }
    }
}
