package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidEventImageKeyException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_EVENT_IMAGE_KEY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidEventImageKeyException()
    }
}
