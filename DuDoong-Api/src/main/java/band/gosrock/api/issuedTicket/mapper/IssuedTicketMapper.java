package band.gosrock.api.issuedTicket.mapper;


import band.gosrock.api.common.page.PageResponse;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDTO;
import band.gosrock.api.issuedTicket.dto.response.RetrieveIssuedTicketDetailResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor;
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket;
import band.gosrock.domain.domains.issuedTicket.dto.condition.IssuedTicketCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class IssuedTicketMapper {

    private final IssuedTicketAdaptor issuedTicketAdaptor;

    private final EventAdaptor eventAdaptor;

    @Transactional(readOnly = true)
    public PageResponse<RetrieveIssuedTicketDTO> toIssuedTicketPageResponse(
            Pageable page, IssuedTicketCondition condition) {
        Page<IssuedTicket> issuedTickets = issuedTicketAdaptor.searchIssuedTicket(page, condition);
        return PageResponse.of(issuedTickets.map(RetrieveIssuedTicketDTO::of));
    }

    @Transactional(readOnly = true)
    public RetrieveIssuedTicketDetailResponse toIssuedTicketDetailResponse(
            Long currentUserId, Long issuedTicketId) {
        IssuedTicket issuedTicket = issuedTicketAdaptor.findForUser(currentUserId, issuedTicketId);
        Event event = eventAdaptor.findById(issuedTicket.getEventId());
        return RetrieveIssuedTicketDetailResponse.of(issuedTicket, event);
    }

    @Transactional(readOnly = true)
    public IssuedTicket getIssuedTicket(Long issuedTicketId) {
        return issuedTicketAdaptor.queryIssuedTicket(issuedTicketId);
    }
}
