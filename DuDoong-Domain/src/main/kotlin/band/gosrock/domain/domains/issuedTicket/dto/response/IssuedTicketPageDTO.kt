package band.gosrock.domain.domains.issuedTicket.dto.response

import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import org.springframework.data.domain.Page

class IssuedTicketPageDTO {
    var issuedTickets: Page<IssuedTicket>? = null
}
