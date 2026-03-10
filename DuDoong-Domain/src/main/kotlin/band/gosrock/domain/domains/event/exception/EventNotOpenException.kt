package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class EventNotOpenException private constructor() : DuDoongCodeException(EventErrorCode.EVENT_NOT_OPEN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EventNotOpenException()
    }
}
