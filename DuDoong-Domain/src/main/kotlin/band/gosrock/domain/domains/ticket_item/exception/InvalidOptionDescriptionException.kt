package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidOptionDescriptionException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_OPTION_DESCRIPTION) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidOptionDescriptionException()
    }
}
