package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotPartnerHostException private constructor() : DuDoongCodeException(HostErrorCode.NOT_PARTNER_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotPartnerHostException()
    }
}
