package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.ticket_item.service.TicketItemService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteTicketItemUseCase {

    private final TicketItemMapper ticketItemMapper;
    private final TicketItemService ticketItemService;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    public GetEventTicketItemsResponse execute(Long eventId, Long ticketItemId) {

        ticketItemService.softDeleteTicketItem(eventId, ticketItemId);

        return ticketItemMapper.toGetEventTicketItemsResponse(eventId, true);
    }
}
