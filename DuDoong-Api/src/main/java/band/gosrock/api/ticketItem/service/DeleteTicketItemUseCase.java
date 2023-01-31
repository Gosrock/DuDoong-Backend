package band.gosrock.api.ticketItem.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.ticket_item.service.TicketItemService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteTicketItemUseCase {

    private final TicketItemMapper ticketItemMapper;
    private final UserUtils userUtils;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final HostService hostService;
    private final TicketItemService ticketItemService;

    public GetEventTicketItemsResponse execute(Long eventId, Long ticketItemId) {
        // 권한체크 aop 변경 예정
        User user = userUtils.getCurrentUser();
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        hostService.validateHostUser(host, user.getId());

        ticketItemService.patchTicketItemStatusToDeleted(eventId, ticketItemId);

        return ticketItemMapper.toGetEventTicketItemsResponse(eventId);
    }
}
