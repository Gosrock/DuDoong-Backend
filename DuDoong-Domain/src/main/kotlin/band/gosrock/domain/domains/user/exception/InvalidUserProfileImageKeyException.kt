package band.gosrock.domain.domains.user.exception

import band.gosrock.common.exception.DuDoongCodeException

class InvalidUserProfileImageKeyException private constructor() : DuDoongCodeException(UserErrorCode.USER_PROFILE_IMAGE_KEY_INVALID) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = InvalidUserProfileImageKeyException()
    }
}
