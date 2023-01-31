package band.gosrock.api.ticketItem.service;


import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketOptionsUseCase {

    private final TicketOptionMapper ticketOptionMapper;

    public GetTicketItemOptionsResponse execute(Long eventId, Long ticketItemId) {

        return ticketOptionMapper.toGetTicketItemOptionResponse(eventId, ticketItemId);
    }
}
