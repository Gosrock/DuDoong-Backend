package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenHostException private constructor() : DuDoongCodeException(HostErrorCode.FORBIDDEN_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenHostException()
    }
}
