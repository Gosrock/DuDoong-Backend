package band.gosrock.domain.domains.issuedTicket.dto.response;


import java.util.List;
import lombok.Getter;

@Getter
public class PostIssuedTicketResponse {

    private final List<IssuedTicketDTO> issuedTickets;

    public PostIssuedTicketResponse(List<IssuedTicketDTO> issuedTickets) {
        this.issuedTickets = issuedTickets;
    }
}
