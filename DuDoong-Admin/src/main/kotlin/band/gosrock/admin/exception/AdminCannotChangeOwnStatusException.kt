package band.gosrock.admin.exception

import band.gosrock.common.exception.DuDoongCodeException

class AdminCannotChangeOwnStatusException private constructor() : DuDoongCodeException(AdminErrorCode.ADMIN_CANNOT_CHANGE_OWN_STATUS) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AdminCannotChangeOwnStatusException()
    }
}
