package band.gosrock.api.issuedTicket.mapper;


import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketListResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condition.IssuedTicketCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class IssuedTicketMapper {

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketListResponse toIssuedTicketPageResponse(
            Long page, IssuedTicketCondition condition) {
        return RetrieveIssuedTicketListResponse.of(
                issuedTicketAdaptor.searchIssuedTicket(page, condition));
    }

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketDetailResponse toIssuedTicketDetailResponse(
            Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.findForUser(currentUserId, issuedTicketId);
        Event event = eventAdaptor.findById(issuedTicket.getEventId());
        return RetrieveIssuedTicketDetailResponse.of(
            issuedTicket, event);
    }
}
