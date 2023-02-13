package band.gosrock.domain.domains.issuedTicket.service.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

import band.gosrock.domain.common.events.order.DoneOrderEvent;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class OrderEventHandlerTest {

    @Mock
    IssuedTicketDomainService issuedTicketDomainService;

    @Mock
    DoneOrderEvent doneOrderEvent;

    @Mock
    DoneOrderEvent doneOrderEvent1;

    @Mock
    DoneOrderEvent doneOrderEvent2;

    @Test
    public void 주문이_정상_처리되었으면_티켓발급_서비스를_실행하는지_테스트() {
        OrderEventHandler orderEventHandler = new OrderEventHandler(issuedTicketDomainService);
        //when
        orderEventHandler.handleDoneOrderEvent(doneOrderEvent);

        then(issuedTicketDomainService).should(times(1)).createIssuedTicket(any(), any(), any());
    }

    @Test
    public void 주문이_여러번_정상_처리되었으면_그만큼_티켓발급_서비스를_실행하는지_테스트() {
        OrderEventHandler orderEventHandler = new OrderEventHandler(issuedTicketDomainService);
        //when
        orderEventHandler.handleDoneOrderEvent(doneOrderEvent);
        orderEventHandler.handleDoneOrderEvent(doneOrderEvent1);
        orderEventHandler.handleDoneOrderEvent(doneOrderEvent2);

        then(issuedTicketDomainService).should(times(3)).createIssuedTicket(any(), any(), any());
    }
}
