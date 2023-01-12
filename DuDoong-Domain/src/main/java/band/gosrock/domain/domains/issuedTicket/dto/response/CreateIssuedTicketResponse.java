package band.gosrock.domain.domains.issuedTicket.dto.response;


import java.util.List;
import lombok.Getter;

@Getter
public class CreateIssuedTicketResponse {

    private final List<IssuedTicketDTO> issuedTickets;

    public CreateIssuedTicketResponse(List<IssuedTicketDTO> issuedTickets) {
        this.issuedTickets = issuedTickets;
    }
}
