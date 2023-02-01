package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse;
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse;
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class TicketOptionMapper {

    private final TicketItemAdaptor ticketItemAdaptor;
    private final EventAdaptor eventAdaptor;
    private final OptionGroupAdaptor optionGroupAdaptor;

    public OptionGroup toOptionGroup(
            CreateTicketOptionRequest createTicketOptionRequest, Event event) {
        return OptionGroup.builder()
                .event(event)
                .type(createTicketOptionRequest.getType())
                .name(createTicketOptionRequest.getName())
                .description(createTicketOptionRequest.getDescription())
                .isEssential(true)
                .options(new ArrayList<>())
                .build();
    }

    @Transactional(readOnly = true)
    public GetTicketItemOptionsResponse toGetTicketItemOptionResponse(
            Long eventId, Long ticketItemId) {

        TicketItem ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId);
        ticketItem.validateEventId(eventId);
        List<OptionGroup> optionGroups =
                ticketItem.getItemOptionGroups().stream()
                        .map(ItemOptionGroup::getOptionGroup)
                        .toList();

        return GetTicketItemOptionsResponse.from(
                optionGroups.stream().map(OptionGroupResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public GetEventOptionsResponse toGetEventOptionResponse(Long eventId) {

        Event event = eventAdaptor.findById(eventId);

        List<OptionGroup> optionGroups = optionGroupAdaptor.findAllByEventId(event.getId());

        return GetEventOptionsResponse.from(
                optionGroups.stream().map(OptionGroupResponse::from).toList());
    }
}
