package band.gosrock.domains.issuedTicket;


import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.repository.IssuedTicketRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class IssuedTicketTest {

    @Test
    @DisplayName("발급티켓 생성 로직 테스트")
    public void PostIssuedTicketTest() {

        IssuedTicketRepository mockIssuedTicketRepository =
                Mockito.mock(IssuedTicketRepository.class);
        IssuedTicketAdaptor mockIssuedTicketAdaptor = Mockito.mock(IssuedTicketAdaptor.class);

        /*
        Todo: event 등 관련 entity들 create 로직 생기면 작성
         */

        // given

        // when

        // then
    }
}
