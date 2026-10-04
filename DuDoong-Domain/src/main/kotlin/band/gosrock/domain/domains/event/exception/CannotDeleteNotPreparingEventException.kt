package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotDeleteNotPreparingEventException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_DELETE_NOT_PREPARING_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotDeleteNotPreparingEventException()
    }
}
