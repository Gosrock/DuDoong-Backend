package band.gosrock.api.ticketItem.service

import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetTicketOptionsUseCase(
    private val ticketOptionMapper: TicketOptionMapper,
) {

    fun execute(eventId: Long, ticketItemId: Long): GetTicketItemOptionsResponse =
        ticketOptionMapper.toGetTicketItemOptionResponse(eventId, ticketItemId)
}
