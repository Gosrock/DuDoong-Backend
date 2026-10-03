package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemNotOnSaleException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_NOT_ON_SALE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemNotOnSaleException()
    }
}
