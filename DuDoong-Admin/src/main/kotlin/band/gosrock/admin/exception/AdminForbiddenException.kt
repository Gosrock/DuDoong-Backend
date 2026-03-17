package band.gosrock.admin.exception

import band.gosrock.common.exception.DuDoongCodeException

class AdminForbiddenException private constructor() : DuDoongCodeException(AdminErrorCode.ADMIN_FORBIDDEN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AdminForbiddenException()
    }
}
