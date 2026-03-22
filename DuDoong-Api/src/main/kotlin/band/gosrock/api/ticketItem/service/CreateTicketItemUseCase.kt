package band.gosrock.api.ticketItem.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.EVENT_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse
import band.gosrock.api.ticketItem.mapper.TicketItemMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.ticket_item.service.TicketItemService

@UseCase
class CreateTicketItemUseCase(
    private val eventAdaptor: EventAdaptor,
    private val hostAdaptor: HostAdaptor,
    private val ticketItemService: TicketItemService,
    private val ticketItemMapper: TicketItemMapper,
) {

    @HostRolesAllowed(role = GUEST, findHostFrom = EVENT_ID, applyTransaction = false)
    fun execute(userId: Long, createTicketItemRequest: CreateTicketItemRequest, eventId: Long): TicketItemResponse {
        val event = eventAdaptor.findById(eventId)
        val host = hostAdaptor.findById(event.hostId!!)
        val isPartner = host.partner
        val ticketItem = ticketItemService.createTicketItem(
            ticketItemMapper.toTicketItem(createTicketItemRequest, eventId),
            isPartner,
        )
        return TicketItemResponse.from(ticketItem, true)
    }
}
