package band.gosrock.domain.domains.issuedTicket.dto.request;


import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateIssuedTicketRequestDTOs {

    private List<CreateIssuedTicketRequest> createIssuedTicketRequests;
}
