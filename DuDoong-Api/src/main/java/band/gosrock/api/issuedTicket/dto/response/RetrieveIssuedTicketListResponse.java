package band.gosrock.api.issuedTicket.dto.response;


import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class RetrieveIssuedTicketListResponse {

    private final int page;

    private final int totalPage;

    private final List<RetrieveIssuedTicketDTO> issuedTickets;

    public static RetrieveIssuedTicketListResponse of(Page<IssuedTicket> issuedTickets) {
        return RetrieveIssuedTicketListResponse.builder()
                .page(issuedTickets.getPageable().getPageNumber())
                .totalPage(issuedTickets.getTotalPages())
                .issuedTickets(
                        issuedTickets.stream()
                                .map(
                                    RetrieveIssuedTicketDTO::of)
                                .toList())
                .build();
    }
}
