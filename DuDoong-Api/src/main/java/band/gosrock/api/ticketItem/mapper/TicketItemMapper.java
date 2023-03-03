package band.gosrock.api.ticketItem.mapper;


import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest;
import band.gosrock.api.ticketItem.dto.response.*;
import band.gosrock.common.annotation.Mapper;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.event.adaptor.EventAdaptor;
import band.gosrock.domain.domains.event.domain.Event;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.ItemOptionGroup;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@Mapper
@RequiredArgsConstructor
public class TicketItemMapper {

    private final TicketItemAdaptor ticketItemAdaptor;
    private final EventAdaptor eventAdaptor;

    public TicketItem toTicketItem(CreateTicketItemRequest createTicketItemRequest, Long eventId) {

        return TicketItem.builder()
                .payType(createTicketItemRequest.getPayType())
                .name(createTicketItemRequest.getName())
                .description(createTicketItemRequest.getDescription())
                .price(Money.wons(createTicketItemRequest.getPrice()))
                .quantity(createTicketItemRequest.getSupplyCount())
                .supplyCount(createTicketItemRequest.getSupplyCount())
                .purchaseLimit(createTicketItemRequest.getPurchaseLimit())
                .type(createTicketItemRequest.getApproveType())
                .bankName(createTicketItemRequest.getBankName())
                .accountNumber(createTicketItemRequest.getAccountNumber())
                .accountHolder(createTicketItemRequest.getAccountHolder())
                .isQuantityPublic(createTicketItemRequest.getIsQuantityPublic())
                .isSellable(true)
                .eventId(eventId)
                .build();
    }

    @Transactional(readOnly = true)
    public GetEventTicketItemsResponse toGetEventTicketItemsResponse(
            Long eventId, Boolean isAdmin) {

        Event event = eventAdaptor.findById(eventId);
        List<TicketItem> ticketItems = ticketItemAdaptor.findAllByEventId(event.getId());
        return GetEventTicketItemsResponse.from(
                ticketItems.stream()
                        .map(ticketItem -> TicketItemResponse.from(ticketItem, isAdmin))
                        .toList());
    }

    @Transactional(readOnly = true)
    public GetAppliedOptionGroupsResponse toGetAppliedOptionGroupsResponse(Long eventId) {

        Event event = eventAdaptor.findById(eventId);
        List<TicketItem> ticketItems = ticketItemAdaptor.findAllByEventId(event.getId());
        List<AppliedOptionGroupResponse> appliedOptionGroups = new ArrayList<>();
        ticketItems.forEach(
                ticketItem ->
                        appliedOptionGroups.add(
                                AppliedOptionGroupResponse.from(
                                        ticketItem,
                                        ticketItem.getItemOptionGroups().stream()
                                                .map(ItemOptionGroup::getOptionGroup)
                                                .toList()
                                                .stream()
                                                .map(OptionGroupResponse::from)
                                                .toList())));
        return GetAppliedOptionGroupsResponse.from(appliedOptionGroups);
    }
}
