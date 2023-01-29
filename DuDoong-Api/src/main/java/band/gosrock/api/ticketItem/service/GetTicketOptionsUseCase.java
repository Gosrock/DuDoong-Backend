package band.gosrock.api.ticketItem.service;


import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionResponse;
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetTicketOptionsUseCase {

    private final TicketOptionMapper ticketOptionMapper;

    @RedissonLock(LockName = "티켓상품", identifier = "ticketItemId")
    public GetTicketItemOptionResponse execute(Long eventId, Long ticketItemId) {

        return ticketOptionMapper.toGetTicketItemOptionResponse(eventId, ticketItemId);
    }
}
