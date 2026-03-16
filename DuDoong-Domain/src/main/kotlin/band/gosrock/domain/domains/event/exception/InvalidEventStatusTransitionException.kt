package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidEventStatusTransitionException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_EVENT_STATUS_TRANSITION) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidEventStatusTransitionException()
    }
}
