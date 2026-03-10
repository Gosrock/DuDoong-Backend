package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketPurchaseLimitException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_PURCHASE_LIMIT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketPurchaseLimitException()
    }
}
