package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class HostMasterCannotWithdrawException private constructor() : DuDoongCodeException(UserErrorCode.USER_HOST_MASTER_CANNOT_WITHDRAW) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = HostMasterCannotWithdrawException()
    }
}
