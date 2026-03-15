package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetEventTicketItemsUseCase(
    private val ticketItemMapper: TicketItemMapper,
) {

    fun execute(eventId: Long): GetEventTicketItemsResponse =
        ticketItemMapper.toGetEventTicketItemsResponse(eventId, false)

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun executeForAdmin(eventId: Long): GetEventTicketItemsResponse =
        ticketItemMapper.toGetEventTicketItemsResponse(eventId, true)
}
