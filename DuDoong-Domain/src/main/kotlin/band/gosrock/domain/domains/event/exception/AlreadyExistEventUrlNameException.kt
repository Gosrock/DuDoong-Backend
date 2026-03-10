package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyExistEventUrlNameException private constructor() : DuDoongCodeException(EventErrorCode.EVENT_URL_NAME_ALREADY_EXIST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyExistEventUrlNameException()
    }
}
