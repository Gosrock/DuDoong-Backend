package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class IssuedTicketNotFoundException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.ISSUED_TICKET_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = IssuedTicketNotFoundException()
    }
}
