package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotRemoveMasterException private constructor() : DuDoongCodeException(HostErrorCode.CANNOT_REMOVE_MASTER) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotRemoveMasterException()
    }
}
