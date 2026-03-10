package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidTicketTypeException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_TICKET_TYPE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTicketTypeException()
    }
}
