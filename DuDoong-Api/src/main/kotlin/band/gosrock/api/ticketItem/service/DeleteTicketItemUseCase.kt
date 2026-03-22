package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.ticket_item.service.TicketItemService

@UseCase
class DeleteTicketItemUseCase(
    private val ticketItemMapper: TicketItemMapper,
    private val ticketItemService: TicketItemService,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, eventId: Long, ticketItemId: Long): GetEventTicketItemsResponse {
        ticketItemService.softDeleteTicketItem(eventId, ticketItemId)
        return ticketItemMapper.toGetEventTicketItemsResponse(eventId, true)
    }
}
