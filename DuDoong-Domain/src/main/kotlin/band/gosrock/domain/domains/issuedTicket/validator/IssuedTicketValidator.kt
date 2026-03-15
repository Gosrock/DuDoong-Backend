package band.gosrock.domain.domains.issuedTicket.validator

import band.gosrock.common.annotation.Validator
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.exception.IssuedTicketNotMatchedEventException

@Validator
class IssuedTicketValidator {

    fun validIssuedTicketEventIdEqualEvent(issuedTicket: IssuedTicket, eventId: Long) {
        if (issuedTicket.eventId != eventId) {
            throw IssuedTicketNotMatchedEventException.EXCEPTION
        }
    }
}
