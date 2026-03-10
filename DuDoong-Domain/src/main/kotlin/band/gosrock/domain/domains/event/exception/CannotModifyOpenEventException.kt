package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotModifyOpenEventException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_MODIFY_OPEN_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotModifyOpenEventException()
    }
}
