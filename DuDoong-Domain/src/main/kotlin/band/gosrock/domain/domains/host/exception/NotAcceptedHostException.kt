package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotAcceptedHostException private constructor() : DuDoongCodeException(HostErrorCode.NOT_ACCEPTED_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotAcceptedHostException()
    }
}
