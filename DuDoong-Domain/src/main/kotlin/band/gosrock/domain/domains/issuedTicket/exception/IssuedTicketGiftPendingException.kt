package band.gosrock.domain.domains.issuedTicket.exception

import band.gosrock.common.exception.DuDoongCodeException

class IssuedTicketGiftPendingException private constructor() : DuDoongCodeException(IssuedTicketErrorCode.ISSUED_TICKET_GIFT_PENDING) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = IssuedTicketGiftPendingException()
    }
}
