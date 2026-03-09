package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidSlackUrlException private constructor() : DuDoongCodeException(HostErrorCode.INVALID_SLACK_URL) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidSlackUrlException()
    }
}
