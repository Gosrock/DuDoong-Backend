package band.gosrock.domain.domains.issuedTicket.service.handlers;


import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class WithdrawOrderEventHandler {

    private final IssuedTicketDomainService issuedTicketDomainService;

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.BEFORE_COMMIT)
    public void handleWithdrawOrderEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getOrderUuid() + "주문 상태 철회 , 티켓 철회 필요");
        List<IssuedTicket> issuedTickets =
                issuedTicketAdaptor.findAllByOrderUuid(withDrawOrderEvent.getOrderUuid());
        issuedTicketDomainService.withdrawIssuedTicket(
                withDrawOrderEvent.getItemId(), issuedTickets);
    }
}
