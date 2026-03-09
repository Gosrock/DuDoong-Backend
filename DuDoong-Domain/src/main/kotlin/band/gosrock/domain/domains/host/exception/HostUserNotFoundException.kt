package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class HostUserNotFoundException private constructor() : DuDoongCodeException(HostErrorCode.HOST_USER_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = HostUserNotFoundException()
    }
}
