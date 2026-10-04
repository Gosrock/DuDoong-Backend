package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidEventTagException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_EVENT_TAG) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidEventTagException()
    }
}
