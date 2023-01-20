package band.gosrock.api.ticketItem.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.CreateTicketItemResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.host.service.HostService;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import band.gosrock.domain.domains.ticket_item.service.TicketItemService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateTicketItemUseCase {

    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final UserUtils userUtils;
    private final HostService hostService;
    private final TicketItemService ticketItemService;

    @Transactional
    public CreateTicketItemResponse execute(CreateTicketItemRequest createTicketItemRequest) {
        User user = userUtils.getCurrentUser();
        Event event = eventAdaptor.findById(createTicketItemRequest.getEventId());
        Host host = hostAdaptor.findById(event.getHostId());
        // 권한 체크 ( 해당 이벤트의 호스트인지 )
        host.hasHostUserId(user.getId());
        // 호스트 제휴 여부에 따른 가격 체크
        if (!host.getPartner()) {
            ticketItemService.checkTicketPrice(Money.wons(createTicketItemRequest.getPrice()));
        }
        TicketItem ticketItem =
                ticketItemService.createTicketItem(createTicketItemRequest.toEntity(event));
        return CreateTicketItemResponse.from(ticketItem);
    }
}
