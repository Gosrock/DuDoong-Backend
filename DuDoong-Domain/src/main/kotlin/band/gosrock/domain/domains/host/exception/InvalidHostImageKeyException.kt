package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidHostImageKeyException private constructor() : DuDoongCodeException(HostErrorCode.INVALID_HOST_IMAGE_KEY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidHostImageKeyException()
    }
}
