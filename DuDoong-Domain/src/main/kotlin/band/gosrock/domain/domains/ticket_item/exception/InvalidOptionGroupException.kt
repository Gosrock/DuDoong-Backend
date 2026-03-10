package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidOptionGroupException private constructor() : DuDoongCodeException(TicketItemErrorCode.INVALID_OPTION_GROUP) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidOptionGroupException()
    }
}
