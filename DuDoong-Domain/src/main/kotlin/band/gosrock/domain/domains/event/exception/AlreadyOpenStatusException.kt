package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyOpenStatusException private constructor() : DuDoongCodeException(EventErrorCode.ALREADY_OPEN_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyOpenStatusException()
    }
}
