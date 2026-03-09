package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class NotMasterHostException private constructor() : DuDoongCodeException(HostErrorCode.NOT_MASTER_HOST) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = NotMasterHostException()
    }
}
