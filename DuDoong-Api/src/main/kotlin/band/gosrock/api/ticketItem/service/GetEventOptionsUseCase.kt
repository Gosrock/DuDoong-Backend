package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetEventOptionsUseCase(
    private val ticketOptionMapper: TicketOptionMapper,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): GetEventOptionsResponse =
        ticketOptionMapper.toGetEventOptionResponse(eventId)
}
