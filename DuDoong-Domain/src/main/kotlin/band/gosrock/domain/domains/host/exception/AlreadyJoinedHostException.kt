package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyJoinedHostException private constructor() : DuDoongCodeException(HostErrorCode.ALREADY_JOINED_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyJoinedHostException()
    }
}
