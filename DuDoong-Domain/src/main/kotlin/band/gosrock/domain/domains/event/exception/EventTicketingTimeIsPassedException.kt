package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class EventTicketingTimeIsPassedException private constructor() : DuDoongCodeException(EventErrorCode.EVENT_TICKETING_TIME_IS_PASSED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EventTicketingTimeIsPassedException()
    }
}
