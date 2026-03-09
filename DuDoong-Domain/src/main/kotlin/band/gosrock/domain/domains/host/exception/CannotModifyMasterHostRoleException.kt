package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotModifyMasterHostRoleException private constructor() : DuDoongCodeException(HostErrorCode.CANNOT_MODIFY_MASTER_HOST_ROLE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotModifyMasterHostRoleException()
    }
}
