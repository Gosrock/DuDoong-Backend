package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class IssuedTicketUserNotMatchedException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.ISSUED_TICKET_NOT_MATCHED_USER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = IssuedTicketUserNotMatchedException()
    }
}
