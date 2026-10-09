package band.gosrock.admin.exception

import band.gosrock.common.exception.DuDoongCodeException

class AdminSuperAdminRequiredException private constructor() : DuDoongCodeException(AdminErrorCode.ADMIN_SUPER_ADMIN_REQUIRED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AdminSuperAdminRequiredException()
    }
}
