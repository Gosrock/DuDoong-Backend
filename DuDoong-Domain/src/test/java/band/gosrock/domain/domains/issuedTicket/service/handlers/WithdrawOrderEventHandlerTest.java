package band.gosrock.domain.domains.issuedTicket.service.handlers;

import static org.mockito.BDDMockito.*;

import band.gosrock.domain.common.events.order.WithDrawOrderEvent;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class WithdrawOrderEventHandlerTest {

    @Mock IssuedTicketDomainService issuedTicketDomainService;

    @Mock IssuedTicketAdaptor issuedTicketAdaptor;

    @Mock IssuedTicket issuedTicket;

    @Mock IssuedTicket issuedTicket1;

    @Mock WithDrawOrderEvent withDrawOrderEvent;

    List<IssuedTicket> issuedTickets = new ArrayList<>();

    private String orderUuid = "ORDERUUID";

    @BeforeEach
    void setUp() {
        issuedTickets.add(issuedTicket);
        issuedTickets.add(issuedTicket1);
    }

    @Test
    public void 주문이_취소되었으면_티켓발급_취소_로직_실행_테스트() {
        WithdrawOrderEventHandler withDrawOrderEventHandler =
                new WithdrawOrderEventHandler(issuedTicketDomainService, issuedTicketAdaptor);
        // given
        given(withDrawOrderEvent.getOrderUuid()).willReturn(orderUuid);
        given(issuedTicketAdaptor.findAllByOrderUuid(orderUuid)).willReturn(issuedTickets);

        // when
        withDrawOrderEventHandler.handleWithdrawOrderEvent(withDrawOrderEvent);

        // then
        then(issuedTicketDomainService).should(times(1)).withdrawIssuedTicket(any(), any());
    }
}
