package band.gosrock.domain.domains.issuedTicket.dto.response;

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
public class IssuedTicketPageDTO {

    Page<IssuedTicket> issuedTickets;


}
