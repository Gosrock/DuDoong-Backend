package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.ticket_item.service.TicketOptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class DeleteOptionGroupUseCase {

    private final TicketOptionMapper ticketOptionMapper;
    private final TicketOptionService ticketOptionService;

    @Transactional
    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    public GetEventOptionsResponse execute(Long eventId, Long optionGroupId) {

        ticketOptionService.softDeleteOptionGroup(eventId, optionGroupId);

        return ticketOptionMapper.toGetEventOptionResponse(eventId);
    }
}
