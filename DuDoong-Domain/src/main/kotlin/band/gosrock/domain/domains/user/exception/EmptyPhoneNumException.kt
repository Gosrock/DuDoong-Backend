package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class EmptyPhoneNumException private constructor() : DuDoongCodeException(UserErrorCode.USER_PHONE_EMPTY) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = EmptyPhoneNumException()
    }
}
