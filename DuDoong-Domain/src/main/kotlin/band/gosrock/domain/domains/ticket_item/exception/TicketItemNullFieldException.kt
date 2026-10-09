package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemNullFieldException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_NULL_FIELD) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemNullFieldException()
    }
}
