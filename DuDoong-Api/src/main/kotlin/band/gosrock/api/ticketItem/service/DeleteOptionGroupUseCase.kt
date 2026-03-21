package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.TicketOptionService

@UseCase
class DeleteOptionGroupUseCase(
    private val ticketOptionMapper: TicketOptionMapper,
    private val ticketOptionService: TicketOptionService,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, optionGroupId: Long): GetEventOptionsResponse {
        ticketOptionService.softDeleteOptionGroup(eventId, optionGroupId)
        return ticketOptionMapper.toGetEventOptionResponse(eventId)
    }
}
