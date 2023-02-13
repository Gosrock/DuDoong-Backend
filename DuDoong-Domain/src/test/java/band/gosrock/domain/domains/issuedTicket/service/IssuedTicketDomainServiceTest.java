package band.gosrock.domain.domains.issuedTicket.service;


import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketDomainServiceTest {

    @Mock IssuedTicket issuedTicket;

    @Mock IssuedTicketAdaptor issuedTicketAdaptor;

    @Mock TicketItemAdaptor ticketItemAdaptor;

    @Mock IssuedTicketValidator issuedTicketValidator;

    @Mock OrderToIssuedTicketService orderToIssuedTicketService;

    @BeforeEach
    void setUp() {}
}
