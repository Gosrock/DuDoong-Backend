package band.gosrock.api.ticketItem.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.ticketItem.dto.request.ApplyTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.ApplyTicketOptionResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.service.ItemOptionGroupService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class ApplyTicketOptionUseCase {

    private final UserUtils userUtils;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final ItemOptionGroupService itemOptionGroupService;
    private final HostService hostService;

    public ApplyTicketOptionResponse execute(
            ApplyTicketOptionRequest applyTicketOptionRequest, Long eventId) {
        User user = userUtils.getCurrentUser();
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        // 권한 체크 ( 해당 이벤트의 호스트인지 )
        hostService.validateHostUser(host, user.getId());

        Long ticketItemId = applyTicketOptionRequest.getTicketItemId();
        Long optionGroupId = applyTicketOptionRequest.getOptionGroupId();

        TicketItem ticketItem =
                itemOptionGroupService.addItemOptionGroup(ticketItemId, optionGroupId, eventId);
        return ApplyTicketOptionResponse.from(ticketItem);
    }
}
