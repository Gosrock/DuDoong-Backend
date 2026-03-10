package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class EventOpenTimeExpiredException private constructor() : DuDoongCodeException(EventErrorCode.OPEN_TIME_EXPIRED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EventOpenTimeExpiredException()
    }
}
