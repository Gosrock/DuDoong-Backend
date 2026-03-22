package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.user.domain.User

class UserProfileVo(
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
    }
}
