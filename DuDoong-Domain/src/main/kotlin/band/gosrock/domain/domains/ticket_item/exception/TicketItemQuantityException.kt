package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemQuantityException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LESS_THAN_ZERO) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemQuantityException()
    }
}
