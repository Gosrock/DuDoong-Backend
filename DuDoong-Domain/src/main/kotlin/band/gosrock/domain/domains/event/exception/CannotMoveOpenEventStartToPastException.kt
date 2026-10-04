package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotMoveOpenEventStartToPastException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_MOVE_OPEN_EVENT_START_TO_PAST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotMoveOpenEventStartToPastException()
    }
}
