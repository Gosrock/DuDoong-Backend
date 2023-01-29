package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import lombok.RequiredArgsConstructor;

@Mapper
@RequiredArgsConstructor
public class TicketItemMapper {

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
}
