package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class TicketDisabledEventException private constructor() : DuDoongCodeException(TicketItemErrorCode.TICKET_DISABLED_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = TicketDisabledEventException()
    }
}
