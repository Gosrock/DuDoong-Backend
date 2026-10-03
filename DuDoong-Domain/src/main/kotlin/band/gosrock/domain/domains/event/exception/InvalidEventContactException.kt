package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidEventContactException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_EVENT_CONTACT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidEventContactException()
    }
}
