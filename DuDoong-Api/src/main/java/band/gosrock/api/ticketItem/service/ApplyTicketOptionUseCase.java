package band.gosrock.api.ticketItem.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.ticketItem.dto.request.ApplyTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.ApplyTicketOptionResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
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
    private final TicketItemAdaptor ticketItemAdaptor;
    private final OptionAdaptor optionAdaptor;
    private final ItemOptionGroupService itemOptionGroupService;
    private final TicketOptionMapper ticketOptionMapper;

    public ApplyTicketOptionResponse execute(
            ApplyTicketOptionRequest applyTicketOptionRequest, Long eventId) {
        User user = userUtils.getCurrentUser();
        Event event = eventAdaptor.findById(eventId);
        Host host = hostAdaptor.findById(event.getHostId());
        // 권한 체크 ( 해당 이벤트의 호스트인지 )
        host.hasHostUserId(user.getId());

        TicketItem ticketItem =
                ticketItemAdaptor.queryTicketItem(applyTicketOptionRequest.getTicketItemId());
        OptionGroup optionGroup =
                optionAdaptor.queryOptionGroup(applyTicketOptionRequest.getOptionGroupId());
        // 티켓상품과 옵션이 해당 이벤트 소속인지 확인
        itemOptionGroupService.checkEventId(eventId, ticketItem, optionGroup);
        // 이미 적용된 옵션인지 확인
        itemOptionGroupService.checkApply(ticketItem, optionGroup);

        ItemOptionGroup itemOptionGroup =
                itemOptionGroupService.createItemOptionGroup(
                        ticketOptionMapper.toItemOptionGroup(ticketItem, optionGroup));
        return ApplyTicketOptionResponse.from(itemOptionGroup);
    }
}
