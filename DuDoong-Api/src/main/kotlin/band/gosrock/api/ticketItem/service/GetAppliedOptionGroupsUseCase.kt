package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase

@UseCase
class GetAppliedOptionGroupsUseCase(
    private val ticketItemMapper: TicketItemMapper,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID)
    fun execute(userId: Long, eventId: Long): GetAppliedOptionGroupsResponse =
        ticketItemMapper.toGetAppliedOptionGroupsResponse(eventId)
}
