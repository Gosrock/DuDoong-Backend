package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class IssuedTicketNotMatchedEventException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.ISSUED_TICKET_NOT_MATCHED_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = IssuedTicketNotMatchedEventException()
    }
}
