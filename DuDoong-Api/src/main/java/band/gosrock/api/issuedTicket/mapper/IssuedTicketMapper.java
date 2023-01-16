package band.gosrock.api.issuedTicket.mapper;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.dto.condtion.IssuedTicketCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class IssuedTicketMapper {

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketListResponse toIssuedTicketPageResponse(
            Long page, IssuedTicketCondition condition) {
        return RetrieveIssuedTicketListResponse.of(
                issuedTicketAdaptor.searchIssuedTicket(page, condition));
    }

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketDetailResponse toIssuedTicketDetailResponse(
            Long currentUserId, Long issuedTicketId) {
        return RetrieveIssuedTicketDetailResponse.of(
                issuedTicketAdaptor.find(currentUserId, issuedTicketId));
    }
}
