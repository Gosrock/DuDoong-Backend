package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class IssuedTicketAlreadyEntranceException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.ISSUED_TICKET_ALREADY_ENTRANCE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = IssuedTicketAlreadyEntranceException()
    }
}
