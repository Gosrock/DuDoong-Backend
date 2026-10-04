package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class CannotAssignMasterRoleException private constructor() : DuDoongCodeException(HostErrorCode.CANNOT_ASSIGN_MASTER_ROLE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = CannotAssignMasterRoleException()
    }
}
