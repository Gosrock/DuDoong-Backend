package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotDisableTicketWithTicketsException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_DISABLE_TICKET_WITH_TICKETS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotDisableTicketWithTicketsException()
    }
}
