package band.gosrock.domain.domains.issuedTicket.service.handlers;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    private final IssuedTicketDomainService issuedTicketDomainService;


    private final OrderAdaptor orderAdaptor;

    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.BEFORE_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 티켓 생성작업 진행");
        Order order = orderAdaptor.findByOrderUuid(doneOrderEvent.getOrderUuid());
        issuedTicketDomainService.createIssuedTicket(order.getItem().getId(), doneOrderEvent.getOrderUuid() , doneOrderEvent.getUserId());
        log.info(doneOrderEvent.getOrderUuid() + "주문 상태 완료, 티켓 생성작업 완료");
    }
}
