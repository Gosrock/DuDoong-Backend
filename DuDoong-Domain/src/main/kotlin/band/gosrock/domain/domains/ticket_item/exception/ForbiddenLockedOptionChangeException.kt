package band.gosrock.domain.domains.ticket_item.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenLockedOptionChangeException private constructor() : DuDoongCodeException(TicketItemErrorCode.FORBIDDEN_LOCKED_OPTION_CHANGE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenLockedOptionChangeException()
    }
}
