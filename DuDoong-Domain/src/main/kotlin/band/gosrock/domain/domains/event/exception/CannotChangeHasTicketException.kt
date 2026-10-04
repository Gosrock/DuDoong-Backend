package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotChangeHasTicketException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_CHANGE_HAS_TICKET) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotChangeHasTicketException()
    }
}
