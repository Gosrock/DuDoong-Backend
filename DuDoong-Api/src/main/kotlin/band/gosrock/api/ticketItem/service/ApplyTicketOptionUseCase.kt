package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.request.ApplyTicketOptionRequest
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse
import band.gosrock.api.ticketItem.mapper.TicketOptionMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.ItemOptionGroupService

@UseCase
class ApplyTicketOptionUseCase(
    private val itemOptionGroupService: ItemOptionGroupService,
    private val ticketOptionMapper: TicketOptionMapper,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(
        userId: Long,
        applyTicketOptionRequest: ApplyTicketOptionRequest,
        eventId: Long,
        ticketItemId: Long,
    ): GetTicketItemOptionsResponse {
        val optionGroupId = applyTicketOptionRequest.optionGroupId!!
        val ticketItem = itemOptionGroupService.addItemOptionGroup(ticketItemId, optionGroupId, eventId)
        return ticketOptionMapper.toGetTicketItemOptionResponse(eventId, ticketItem.id!!)
    }
}
