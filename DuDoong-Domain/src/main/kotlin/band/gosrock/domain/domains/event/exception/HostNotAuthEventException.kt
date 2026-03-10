package band.gosrock.domain.domains.event.exception

import band.gosrock.common.exception.DuDoongCodeException

class HostNotAuthEventException private constructor() : DuDoongCodeException(EventErrorCode.HOST_NOT_AUTH_EVENT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = HostNotAuthEventException()
    }
}
