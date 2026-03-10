package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyPreparingStatusException private constructor() : DuDoongCodeException(EventErrorCode.ALREADY_PREPARING_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyPreparingStatusException()
    }
}
