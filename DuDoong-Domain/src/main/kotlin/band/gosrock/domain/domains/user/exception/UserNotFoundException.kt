package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class UserNotFoundException private constructor() : DuDoongCodeException(UserErrorCode.USER_NOT_FOUND) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = UserNotFoundException()
    }
}
