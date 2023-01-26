package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;

@Mapper
@RequiredArgsConstructor
public class TicketOptionMapper {

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

    public ItemOptionGroup toItemOptionGroup(TicketItem ticketItem, OptionGroup optionGroup) {
        return ItemOptionGroup.builder().item(ticketItem).optionGroup(optionGroup).build();
    }
}
