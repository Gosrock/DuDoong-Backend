package band.gosrock.domain.common.dto

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.User

class ProfileViewDto(
    val id: Long?,
    val profileImage: ImageVo?,
    val name: String?,
) {
    companion object {
        @JvmStatic
        fun from(user: User): ProfileViewDto =
            ProfileViewDto(
                id = user.id,
                name = user.profile?.name,
                profileImage = user.profile?.profileImage,
            )
    }
}
