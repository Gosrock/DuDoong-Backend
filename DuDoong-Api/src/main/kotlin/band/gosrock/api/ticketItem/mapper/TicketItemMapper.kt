package band.gosrock.api.ticketItem.mapper

import band.gosrock.api.ticketItem.dto.request.CreateTicketItemRequest
import band.gosrock.api.ticketItem.dto.response.AppliedOptionGroupResponse
import band.gosrock.api.ticketItem.dto.response.GetAppliedOptionGroupsResponse
import band.gosrock.api.ticketItem.dto.response.GetEventTicketItemsResponse
import band.gosrock.api.ticketItem.dto.response.OptionGroupResponse
import band.gosrock.api.ticketItem.dto.response.TicketItemResponse
import band.gosrock.common.annotation.Mapper
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import org.springframework.transaction.annotation.Transactional

@Mapper
class TicketItemMapper(
    private val ticketItemAdaptor: TicketItemAdaptor,
    private val eventAdaptor: EventAdaptor,
) {

    fun toTicketItem(createTicketItemRequest: CreateTicketItemRequest, eventId: Long): TicketItem =
        TicketItem(
            payType = createTicketItemRequest.payType,
            name = createTicketItemRequest.name,
            description = createTicketItemRequest.description,
            price = Money.wons(createTicketItemRequest.price!!),
            quantity = createTicketItemRequest.supplyCount,
            supplyCount = createTicketItemRequest.supplyCount,
            purchaseLimit = createTicketItemRequest.purchaseLimit,
            type = createTicketItemRequest.approveType,
            bankName = createTicketItemRequest.bankName,
            accountNumber = createTicketItemRequest.accountNumber,
            accountHolder = createTicketItemRequest.accountHolder,
            isQuantityPublic = createTicketItemRequest.isQuantityPublic,
            isSellable = true,
            eventId = eventId,
        )

    @Transactional(readOnly = true)
    fun toGetEventTicketItemsResponse(eventId: Long, isAdmin: Boolean): GetEventTicketItemsResponse {
        val event = eventAdaptor.findById(eventId)
        val ticketItems = ticketItemAdaptor.findAllByEventId(event.id!!)
        return GetEventTicketItemsResponse.from(
            ticketItems.map { TicketItemResponse.from(it, isAdmin) }
        )
    }

    @Transactional(readOnly = true)
    fun toGetAppliedOptionGroupsResponse(eventId: Long): GetAppliedOptionGroupsResponse {
        val event = eventAdaptor.findById(eventId)
        val ticketItems = ticketItemAdaptor.findAllByEventId(event.id!!)
        val appliedOptionGroups = ticketItems.map { ticketItem ->
            AppliedOptionGroupResponse.from(
                ticketItem,
                ticketItem.itemOptionGroups
                    .mapNotNull { it.optionGroup }
                    .map { OptionGroupResponse.from(it) }
            )
        }
        return GetAppliedOptionGroupsResponse.from(appliedOptionGroups)
    }
}
