package band.gosrock.domain.domains.issuedTicket.handlers;


import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.issuedTicket.dto.request.CreateIssuedTicketDTO;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.User;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventHandler {

    private final IssuedTicketDomainService issuedTicketDomainService;

    private final UserAdaptor userAdaptor;
    @Async
    @TransactionalEventListener(
            classes = DoneOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleDoneOrderEvent(DoneOrderEvent doneOrderEvent) {
        log.info(doneOrderEvent.getUuid() + "주문 상태 완료, 티켓 생성작업 진행");
        Order order = doneOrderEvent.getOrder();
        User user = userAdaptor.queryUser(order.getUserId());
        List<CreateIssuedTicketDTO> createIssuedTicketDTOS = order.getOrderLineItems().stream()
            .map(orderLineItem -> new CreateIssuedTicketDTO(order, orderLineItem, user)).toList();
        issuedTicketDomainService.createIssuedTicket(createIssuedTicketDTOS);
        log.info(doneOrderEvent.getUuid() + "주문 상태 완료, 티켓 생성작업 완료");
    }

    @Async
    @TransactionalEventListener(
            classes = WithDrawOrderEvent.class,
            phase = TransactionPhase.AFTER_COMMIT)
    public void handleRegisterUserEvent(WithDrawOrderEvent withDrawOrderEvent) {
        log.info(withDrawOrderEvent.getUuid() + "주문 상태 철회 , 티켓 제거 필요");
    }
}
