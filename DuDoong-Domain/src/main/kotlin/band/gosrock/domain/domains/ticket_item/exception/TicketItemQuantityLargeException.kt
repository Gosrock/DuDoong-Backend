package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemQuantityLargeException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_QUANTITY_LARGER_THAN_SUPPLY_COUNT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemQuantityLargeException()
    }
}
