package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidTicketPriceException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_TICKET_PRICE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTicketPriceException()
    }
}
