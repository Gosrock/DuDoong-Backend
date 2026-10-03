package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotModifyEndedEventException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_MODIFY_ENDED_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotModifyEndedEventException()
    }
}
