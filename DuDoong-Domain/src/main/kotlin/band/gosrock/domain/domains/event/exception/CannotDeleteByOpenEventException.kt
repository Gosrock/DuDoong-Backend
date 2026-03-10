package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotDeleteByOpenEventException private constructor() : DuDoongCodeException(EventErrorCode.CANNOT_DELETE_BY_OPEN_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotDeleteByOpenEventException()
    }
}
