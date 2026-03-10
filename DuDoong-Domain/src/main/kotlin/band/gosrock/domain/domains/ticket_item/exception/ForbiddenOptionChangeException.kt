package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenOptionChangeException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_OPTION_CHANGE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenOptionChangeException()
    }
}
