package band.gosrock.api.ticketItem.service

import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetEventOptionsUseCase(
    private val ticketOptionMapper: TicketOptionMapper,
) {

    fun execute(eventId: Long): GetEventOptionsResponse =
        ticketOptionMapper.toGetEventOptionResponse(eventId)
}
