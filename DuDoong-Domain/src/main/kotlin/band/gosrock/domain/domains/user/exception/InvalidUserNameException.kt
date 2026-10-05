package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidUserNameException private constructor() : DuDoongCodeException(UserErrorCode.USER_NAME_INVALID) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidUserNameException()
    }
}
