package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class EventNotFoundException private constructor() : DuDoongCodeException(EventErrorCode.EVENT_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EventNotFoundException()
    }
}
