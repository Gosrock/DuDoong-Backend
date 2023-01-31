package band.gosrock.api.ticketItem.service;


import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetEventTicketItemsUseCase {

    private final TicketItemMapper ticketItemMapper;

    public GetEventTicketItemsResponse execute(Long eventId) {

        return ticketItemMapper.toGetEventTicketItemsResponse(eventId);
    }
}
