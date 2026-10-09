package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class DeletedUserStateChangeException private constructor() : DuDoongCodeException(UserErrorCode.USER_DELETED_STATE_IMMUTABLE) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = DeletedUserStateChangeException()
    }
}
