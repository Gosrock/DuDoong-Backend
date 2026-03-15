package band.gosrock.domain.domains.issuedTicket.dto.request

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.user.domain.User

class CreateIssuedTicketDTO(
    val order: Order,
    val orderLineItem: OrderLineItem,
    val user: User,
)
