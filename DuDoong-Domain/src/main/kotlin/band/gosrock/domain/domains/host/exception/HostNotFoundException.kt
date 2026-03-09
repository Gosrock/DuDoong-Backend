package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class HostNotFoundException private constructor() : DuDoongCodeException(HostErrorCode.HOST_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = HostNotFoundException()
    }
}
