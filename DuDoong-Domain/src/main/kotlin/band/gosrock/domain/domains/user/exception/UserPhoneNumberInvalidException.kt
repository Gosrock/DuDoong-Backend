package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class UserPhoneNumberInvalidException private constructor() : DuDoongCodeException(UserErrorCode.USER_PHONE_INVALID) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = UserPhoneNumberInvalidException()
    }
}
