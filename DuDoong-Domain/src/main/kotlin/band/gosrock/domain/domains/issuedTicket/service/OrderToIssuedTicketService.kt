package band.gosrock.domain.domains.issuedTicket.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.user.adaptor.UserAdaptor

@DomainService
class OrderToIssuedTicketService(
    private val userAdaptor: UserAdaptor,
    private val orderAdaptor: OrderAdaptor,
) {
    fun execute(ticketItem: TicketItem, orderUuid: String, userId: Long): List<IssuedTicket> {
        val user = userAdaptor.queryUser(userId)
        val order = orderAdaptor.findByOrderUuid(orderUuid)
        val eventId = ticketItem.eventId
        return order.orderLineItems.flatMap { orderLineItem ->
            val quantity = orderLineItem.quantity!!
            (0 until quantity).map {
                IssuedTicket.create(ticketItem, user, order, eventId, orderLineItem)
            }
        }
    }
}
