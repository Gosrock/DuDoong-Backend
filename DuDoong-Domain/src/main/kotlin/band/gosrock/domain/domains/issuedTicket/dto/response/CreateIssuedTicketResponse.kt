package band.gosrock.domain.domains.issuedTicket.dto.response

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketOptionAnswer

class CreateIssuedTicketResponse(
    val issuedTickets: List<IssuedTicket>,
    val issuedTicketOptionAnswers: List<IssuedTicketOptionAnswer>,
)
