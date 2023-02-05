package band.gosrock.domain.domains.issuedTicket.service.handlers;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    private final IssuedTicketDomainService issuedTicketDomainService;

    @EventListener(classes = DoneOrderEvent.class)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 티켓 생성작업 진행");
        issuedTicketDomainService.createIssuedTicket(
                doneOrderEvent.getItemId(),
                doneOrderEvent.getOrderUuid(),
                doneOrderEvent.getUserId());
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 티켓 생성작업 완료");
    }
}
