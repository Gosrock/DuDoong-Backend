package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class AlreadyDeletedUserException private constructor() : DuDoongCodeException(UserErrorCode.USER_ALREADY_DELETED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = AlreadyDeletedUserException()
    }
}
