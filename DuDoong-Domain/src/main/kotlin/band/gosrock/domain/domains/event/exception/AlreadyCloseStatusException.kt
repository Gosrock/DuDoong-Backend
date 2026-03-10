package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyCloseStatusException private constructor() : DuDoongCodeException(EventErrorCode.ALREADY_CLOSE_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyCloseStatusException()
    }
}
