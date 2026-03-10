package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenTicketItemDeleteException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_TICKET_ITEM_DELETE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenTicketItemDeleteException()
    }
}
