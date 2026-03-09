package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class DuplicateSlackUrlException private constructor() : DuDoongCodeException(HostErrorCode.DUPLICATED_SLACK_URL) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = DuplicateSlackUrlException()
    }
}
