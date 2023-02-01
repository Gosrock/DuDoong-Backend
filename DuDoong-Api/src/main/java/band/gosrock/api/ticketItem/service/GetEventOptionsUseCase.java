package band.gosrock.api.ticketItem.service;


import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetEventOptionsUseCase {

    private final TicketOptionMapper ticketOptionMapper;

    public GetEventOptionsResponse execute(Long eventId) {

        return ticketOptionMapper.toGetEventOptionResponse(eventId);
    }
}
