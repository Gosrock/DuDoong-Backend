package band.gosrock.domain.domains.host.exception

import band.gosrock.common.exception.DuDoongCodeException

class ManagerCanManageGuestOnlyException private constructor() : DuDoongCodeException(HostErrorCode.MANAGER_CAN_MANAGE_GUEST_ONLY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ManagerCanManageGuestOnlyException()
    }
}
