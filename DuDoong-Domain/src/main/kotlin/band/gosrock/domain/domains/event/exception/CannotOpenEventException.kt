package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotOpenEventException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_OPEN_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotOpenEventException()
    }
}
