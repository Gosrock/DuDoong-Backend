package band.gosrock.api.issuedTicket.dto.response;


import java.util.List;
import lombok.Getter;

@Getter
public class RetrieveIssuedTicketListResponse {

    private final Long page;

    private final Long totalPage;

    private final List<RetrieveIssuedTicketDTO> issuedTickets;

    public RetrieveIssuedTicketListResponse(
            Long page, Long totalPage, List<RetrieveIssuedTicketDTO> issuedTickets) {
        this.page = page;
        this.totalPage = totalPage;
        this.issuedTickets = issuedTickets;
    }
}
