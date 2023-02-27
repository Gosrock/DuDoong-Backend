package band.gosrock.api.ticketItem.service;

import static band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID;
import static band.gosrock.api.common.aop.hostRole.HostQualification.GUEST;

import band.gosrock.api.common.aop.hostRole.HostRolesAllowed;
import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.service.TicketItemService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateTicketItemUseCase {

    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final TicketItemService ticketItemService;
    private final TicketItemMapper ticketItemMapper;

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    public TicketItemResponse execute(
            CreateTicketItemRequest createTicketItemRequest, Long eventId) {

        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());

        // 호스트 제휴 여부
        Boolean isPartner = host.getPartner();
        TicketItem ticketItem =
                ticketItemService.createTicketItem(
                        ticketItemMapper.toTicketItem(createTicketItemRequest, eventId), isPartner);

        return TicketItemResponse.from(ticketItem, true);
    }
}
