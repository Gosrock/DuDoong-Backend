package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import java.math.BigDecimal
import java.time.LocalDateTime

data class AdminTicketItemResponse(
    val id: Long,
    val name: String?,
    val description: String?,
    val price: BigDecimal?,
    val quantity: Long?,
    val supplyCount: Long?,
    val purchaseLimit: Long?,
    val payType: TicketPayType?,
    val type: TicketType?,
    val isQuantityPublic: Boolean?,
    val isSellable: Boolean?,
    val saleStartAt: LocalDateTime?,
    val saleEndAt: LocalDateTime?,
    val ticketItemStatus: TicketItemStatus,
    val eventId: Long?,
) {
    companion object {
        fun from(ticketItem: TicketItem): AdminTicketItemResponse =
            AdminTicketItemResponse(
                id = ticketItem.id!!,
                name = ticketItem.name,
                description = ticketItem.description,
                price = ticketItem.price?.amount,
                quantity = ticketItem.quantity,
                supplyCount = ticketItem.supplyCount,
                purchaseLimit = ticketItem.purchaseLimit,
                payType = ticketItem.payType,
                type = ticketItem.type,
                isQuantityPublic = ticketItem.isQuantityPublic,
                isSellable = ticketItem.isSellable,
                saleStartAt = ticketItem.saleStartAt,
                saleEndAt = ticketItem.saleEndAt,
                ticketItemStatus = ticketItem.ticketItemStatus,
                eventId = ticketItem.eventId,
            )
    }
}
