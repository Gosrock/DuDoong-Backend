package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotDeleteByIssuedTicketException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_DELETE_BY_ISSUED_TICKET) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotDeleteByIssuedTicketException()
    }
}
