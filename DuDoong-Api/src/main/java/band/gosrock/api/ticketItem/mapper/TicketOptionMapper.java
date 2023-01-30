package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse;
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.domain.Event;
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
        ticketItem.checkEventId(eventId);
        List<OptionGroup> optionGroups =
                ticketItem.getItemOptionGroups().stream()
                        .map(ItemOptionGroup::getOptionGroup)
                        .toList();

        return GetTicketItemOptionsResponse.from(
                optionGroups.stream().map(OptionGroupResponse::from).toList());
    }
}
