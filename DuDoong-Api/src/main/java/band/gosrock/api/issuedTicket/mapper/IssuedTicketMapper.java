package band.gosrock.api.issuedTicket.mapper;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class IssuedTicketMapper {

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketListResponse toIssuedTicketPageResponse(
            Page<IssuedTicket> issuedTickets) {
        return RetrieveIssuedTicketListResponse.of(issuedTickets);
    }

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketDetailResponse toIssuedTicketDetailResponse(
            IssuedTicket issuedTicket) {
        System.out.println(
                "issuedTicket.getIssuedTicketOptionAnswers().size() = "
                        + issuedTicket.getIssuedTicketOptionAnswers().size());
        return RetrieveIssuedTicketDetailResponse.of(issuedTicket);
    }
}
