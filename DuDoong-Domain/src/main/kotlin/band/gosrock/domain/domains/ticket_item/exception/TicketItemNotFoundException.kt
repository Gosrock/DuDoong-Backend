package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketItemNotFoundException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_ITEM_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketItemNotFoundException()
    }
}
