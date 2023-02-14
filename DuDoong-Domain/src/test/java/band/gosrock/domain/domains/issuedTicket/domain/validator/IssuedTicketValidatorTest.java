package band.gosrock.domain.domains.issuedTicket.domain.validator;

import static org.mockito.BDDMockito.given;

import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotMatchedEventException;
import band.gosrock.domain.domains.issuedTicket.validator.IssuedTicketValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class IssuedTicketValidatorTest {

    @Mock IssuedTicket issuedTicket;

    @Mock EventAdaptor eventAdaptor;

    @Mock HostAdaptor hostAdaptor;

    IssuedTicketValidator issuedTicketValidator;

    @BeforeEach
    public void setUp() {
        issuedTicketValidator = new IssuedTicketValidator(eventAdaptor, hostAdaptor);
    }

    @Test
    public void 발급티켓_이벤트_검증_성공() {
        // given
        given(issuedTicket.getEventId()).willReturn(1L);
        // when
        issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, 1L);
    }

    @Test
    public void 발급티켓_이벤트_검증_실패() {
        // given
        given(issuedTicket.getEventId()).willReturn(2L);
        // when
        // then
        Assertions.assertThrows(
                IssuedTicketNotMatchedEventException.class,
                () -> issuedTicketValidator.validIssuedTicketEventIdEqualEvent(issuedTicket, 1L));
    }
}
