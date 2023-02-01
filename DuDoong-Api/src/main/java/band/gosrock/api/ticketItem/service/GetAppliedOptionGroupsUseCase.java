package band.gosrock.api.ticketItem.service;


import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse;
import band.gosrock.api.ticketItem.mapper.TicketItemMapper;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetAppliedOptionGroupsUseCase {

    private final TicketItemMapper ticketItemMapper;

    public GetAppliedOptionGroupsResponse execute(Long eventId) {

        return ticketItemMapper.toGetAppliedOptionGroupsResponse(eventId);
    }
}
