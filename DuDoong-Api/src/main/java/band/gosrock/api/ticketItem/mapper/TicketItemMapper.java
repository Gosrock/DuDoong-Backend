package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class TicketItemMapper {

    private final TicketItemAdaptor ticketItemAdaptor;
    private final EventAdaptor eventAdaptor;

    public TicketItem toTicketItem(CreateTicketItemRequest createTicketItemRequest, Event event) {

        return TicketItem.builder()
                .type(createTicketItemRequest.getType())
                .name(createTicketItemRequest.getName())
                .description(createTicketItemRequest.getDescription())
                .price(Money.wons(createTicketItemRequest.getPrice()))
                .quantity(createTicketItemRequest.getSupplyCount())
                .supplyCount(createTicketItemRequest.getSupplyCount())
                .purchaseLimit(createTicketItemRequest.getPurchaseLimit())
                .isSellable(true)
                .event(event)
                .build();
    }

    @Transactional(readOnly = true)
    public GetEventTicketItemsResponse toGetEventTicketItemsResponse(Long eventId) {

        Event event = eventAdaptor.findById(eventId);
        List<TicketItem> ticketItems = ticketItemAdaptor.findAllByEventId(event.getId());
        return GetEventTicketItemsResponse.from(
                ticketItems.stream().map(TicketItemResponse::from).toList());
    }
}
