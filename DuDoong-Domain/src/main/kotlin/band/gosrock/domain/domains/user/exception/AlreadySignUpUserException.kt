package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadySignUpUserException private constructor() : DuDoongCodeException(UserErrorCode.USER_ALREADY_SIGNUP) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadySignUpUserException()
    }
}
