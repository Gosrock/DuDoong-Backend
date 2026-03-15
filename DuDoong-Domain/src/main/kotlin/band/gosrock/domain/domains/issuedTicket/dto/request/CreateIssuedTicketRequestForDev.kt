package band.gosrock.domain.domains.issuedTicket.dto.request

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.user.domain.User

class CreateIssuedTicketRequestForDev(
    val event: Event,
    val orderLineId: Long,
    val user: User,
    val price: Money,
    val ticketItem: TicketItem,
    val issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer>,
)
