package band.gosrock.domain.domains.issuedTicket.service.handlers;


import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithDrawOrderEventHandler {

    private final IssuedTicketDomainService issuedTicketDomainService;

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final OrderAdaptor orderAdaptor;

    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.BEFORE_COMMIT)
    public void handleWithDrawOrderEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getOrderUuid() + "주문 상태 철회 , 티켓 제거 필요");
        Order order = orderAdaptor.findByOrderUuid(withDrawOrderEvent.getOrderUuid());
        List<IssuedTicket> issuedTickets =
                issuedTicketAdaptor.findAllByOrderUuid(withDrawOrderEvent.getOrderUuid());
        issuedTicketDomainService.withDrawIssuedTicket(order.getItem(), issuedTickets);
        log.info(withDrawOrderEvent.getOrderUuid() + "주문 상태 완료, 티켓 생성작업 완료");
    }
}
