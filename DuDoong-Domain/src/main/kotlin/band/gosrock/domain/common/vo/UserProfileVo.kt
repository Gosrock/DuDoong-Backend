package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.user.domain.User

class UserProfileVo private constructor(
    val userId: Long?,
    val userName: String?,
    val email: String?,
    val profileImage: ImageVo?,
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserProfileVo =
            UserProfileVo(
                userId = user.id,
                userName = user.profile?.name,
                email = user.profile?.email,
                profileImage = user.profile?.profileImage,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var userName: String? = null
        private var email: String? = null
        private var profileImage: ImageVo? = null

        fun userId(v: Long?) = apply { userId = v }
        fun userName(v: String?) = apply { userName = v }
        fun email(v: String?) = apply { email = v }
        fun profileImage(v: ImageVo?) = apply { profileImage = v }
        fun build() = UserProfileVo(userId, userName, email, profileImage)
    }
}
