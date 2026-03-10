package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class EventCannotEndBeforeStartException private constructor() : DuDoongCodeException(EventErrorCode.EVENT_CANNOT_END_BEFORE_START) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EventCannotEndBeforeStartException()
    }
}
