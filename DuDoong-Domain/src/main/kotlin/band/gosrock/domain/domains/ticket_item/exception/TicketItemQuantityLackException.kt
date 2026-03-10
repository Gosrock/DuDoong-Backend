package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemQuantityLackException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LACK) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemQuantityLackException()
    }
}
