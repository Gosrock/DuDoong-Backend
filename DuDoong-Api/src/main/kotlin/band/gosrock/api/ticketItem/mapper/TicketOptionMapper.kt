package band.gosrock.api.ticketItem.mapper

import band.gosrock.api.ticketItem.dto.request.CreateTicketOptionRequest
import band.gosrock.api.ticketItem.dto.response.GetEventOptionsResponse
import band.gosrock.api.ticketItem.dto.response.GetTicketItemOptionsResponse
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.OptionGroupAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.OptionGroup
import org.springframework.transaction.annotation.Transactional

@Mapper
class TicketOptionMapper(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val optionGroupAdaptor: OptionGroupAdaptor,
) {

    fun toOptionGroup(createTicketOptionRequest: CreateTicketOptionRequest, eventId: Long): OptionGroup =
        OptionGroup(
            eventId = eventId,
            type = createTicketOptionRequest.type,
            name = createTicketOptionRequest.name,
            description = createTicketOptionRequest.description,
            isEssential = true,
            initialOptions = emptyList(),
        )

    @Transactional(readOnly = true)
    fun toGetTicketItemOptionResponse(eventId: Long, ticketItemId: Long): GetTicketItemOptionsResponse {
        val ticketItem = ticketItemAdaptor.queryTicketItem(ticketItemId)
        ticketItem.validateEventId(eventId)
        val optionGroups = ticketItem.itemOptionGroups.mapNotNull { it.optionGroup }
        return GetTicketItemOptionsResponse.from(optionGroups.map { OptionGroupResponse.from(it) })
    }

    @Transactional(readOnly = true)
    fun toGetEventOptionResponse(eventId: Long): GetEventOptionsResponse {
        val event = eventAdaptor.findById(eventId)
        val optionGroups = optionGroupAdaptor.findAllByEventId(event.id!!)
        return GetEventOptionsResponse.from(optionGroups.map { OptionGroupResponse.from(it) })
    }
}
