package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotCheckInEventStatusException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_CHECK_IN_EVENT_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotCheckInEventStatusException()
    }
}
