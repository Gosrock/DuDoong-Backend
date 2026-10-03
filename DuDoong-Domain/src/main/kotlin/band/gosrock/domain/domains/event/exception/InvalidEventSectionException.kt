package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidEventSectionException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_EVENT_SECTION) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidEventSectionException()
    }
}
