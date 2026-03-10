package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidTicketItemException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_TICKET_ITEM) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTicketItemException()
    }
}
