package band.gosrock.api.ticketItem.service

import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetAppliedOptionGroupsUseCase(
    private val ticketItemMapper: TicketItemMapper,
) {

    fun execute(eventId: Long): GetAppliedOptionGroupsResponse =
        ticketItemMapper.toGetAppliedOptionGroupsResponse(eventId)
}
