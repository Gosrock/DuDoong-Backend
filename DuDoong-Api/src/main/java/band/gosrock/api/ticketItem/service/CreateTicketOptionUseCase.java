package band.gosrock.api.ticketItem.service;


import band.gosrock.api.common.UserUtils;
import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.CreateTicketOptionResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.host.adaptor.HostAdaptor;
import band.gosrock.domain.domains.host.domain.Host;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.service.TicketOptionService;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class CreateTicketOptionUseCase {

    private final UserUtils userUtils;
    private final EventAdaptor eventAdaptor;
    private final HostAdaptor hostAdaptor;
    private final TicketOptionMapper ticketOptionMapper;
    private final TicketOptionService ticketOptionService;

    @Transactional
    public CreateTicketOptionResponse execute(
            CreateTicketOptionRequest createTicketOptionRequest, Long eventId) {
        User user = userUtils.getCurrentUser();
        Event event = eventAdaptor.findById(eventId);

        Host host = hostAdaptor.findById(event.getHostId());
        // 권한 체크 ( 해당 이벤트의 호스트인지 )
        host.hasHostUserId(user.getId());
        OptionGroup ticketOption =
                ticketOptionMapper
                        .toOptionGroup(createTicketOptionRequest, event)
                        .createTicketOption(
                                Money.wons(createTicketOptionRequest.getAdditionalPrice()));
        OptionGroup ticketOptionResult = ticketOptionService.createTicketOption(ticketOption);
        return CreateTicketOptionResponse.from(ticketOptionResult);
    }
}
