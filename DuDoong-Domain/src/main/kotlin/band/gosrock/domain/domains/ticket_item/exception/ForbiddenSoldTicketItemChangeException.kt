package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenSoldTicketItemChangeException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_SOLD_TICKET_ITEM_CHANGE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenSoldTicketItemChangeException()
    }
}
