package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidHostContactException private constructor() : DuDoongCodeException(HostErrorCode.INVALID_HOST_CONTACT) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidHostContactException()
    }
}
