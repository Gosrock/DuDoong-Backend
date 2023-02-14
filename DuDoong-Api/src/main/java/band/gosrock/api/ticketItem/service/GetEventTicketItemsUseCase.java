package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetEventTicketItemsUseCase {

    private final TicketItemMapper ticketItemMapper;

    public GetEventTicketItemsResponse execute(Long eventId) {

        return ticketItemMapper.toGetEventTicketItemsResponse(eventId, false);
    }

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    public GetEventTicketItemsResponse executeForAdmin(Long eventId) {

        return ticketItemMapper.toGetEventTicketItemsResponse(eventId, true);
    }
}
