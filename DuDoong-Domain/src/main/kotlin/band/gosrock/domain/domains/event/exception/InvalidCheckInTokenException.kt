package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidCheckInTokenException private constructor() : DuDoongCodeException(EventErrorCode.INVALID_CHECK_IN_TOKEN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidCheckInTokenException()
    }
}
