package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class ForbiddenUserException private constructor() : DuDoongCodeException(UserErrorCode.USER_FORBIDDEN) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = ForbiddenUserException()
    }
}
