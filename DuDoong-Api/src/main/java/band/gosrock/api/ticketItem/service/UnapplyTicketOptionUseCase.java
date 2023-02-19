package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.request.UnapplyTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.service.ItemOptionGroupService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UnapplyTicketOptionUseCase {

    private final ItemOptionGroupService itemOptionGroupService;
    private final TicketOptionMapper ticketOptionMapper;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    public GetTicketItemOptionsResponse execute(
            UnapplyTicketOptionRequest unapplyTicketOptionRequest,
            Long eventId,
            Long ticketItemId) {

        Long optionGroupId = unapplyTicketOptionRequest.getOptionGroupId();

        TicketItem ticketItem =
                itemOptionGroupService.removeItemOptionGroup(ticketItemId, optionGroupId, eventId);

        return ticketOptionMapper.toGetTicketItemOptionResponse(eventId, ticketItem.getId());
    }
}
