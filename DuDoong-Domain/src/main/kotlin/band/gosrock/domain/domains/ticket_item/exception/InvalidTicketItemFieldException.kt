package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidTicketItemFieldException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_TICKET_ITEM_FIELD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTicketItemFieldException()
    }
}
