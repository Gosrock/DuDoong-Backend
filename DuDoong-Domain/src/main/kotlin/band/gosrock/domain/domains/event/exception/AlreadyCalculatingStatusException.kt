package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyCalculatingStatusException private constructor() : DuDoongCodeException(EventErrorCode.ALREADY_CALCULATING_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyCalculatingStatusException()
    }
}
