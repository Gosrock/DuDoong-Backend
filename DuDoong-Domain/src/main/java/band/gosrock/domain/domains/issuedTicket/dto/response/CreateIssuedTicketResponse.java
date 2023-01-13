package band.gosrock.domain.domains.issuedTicket.dto.response;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer;
import java.util.List;
import lombok.Getter;

@Getter
public class CreateIssuedTicketResponse {

    private final List<IssuedTicket> issuedTickets;

    private final List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers;

    public CreateIssuedTicketResponse(
            List<IssuedTicket> issuedTickets,
            List<IssuedTicketOptionAnswer> issuedTicketOptionAnswers) {
        this.issuedTickets = issuedTickets;
        this.issuedTicketOptionAnswers = issuedTicketOptionAnswers;
    }
}
