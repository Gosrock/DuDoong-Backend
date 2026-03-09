package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotManagerHostException private constructor() : DuDoongCodeException(HostErrorCode.NOT_MANAGER_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotManagerHostException()
    }
}
