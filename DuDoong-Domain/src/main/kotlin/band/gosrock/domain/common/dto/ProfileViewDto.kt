package band.gosrock.domain.common.dto

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.User

class ProfileViewDto private constructor(
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

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var id: Long? = null
        private var email: String? = null
        private var phoneNumber: String? = null
        private var profileImage: ImageVo? = null
        private var name: String? = null

        fun id(v: Long?) = apply { id = v }
        fun email(v: String?) = apply { email = v }
        fun phoneNumber(v: String?) = apply { phoneNumber = v }
        fun profileImage(v: ImageVo?) = apply { profileImage = v }
        fun name(v: String?) = apply { name = v }
        fun build() = ProfileViewDto(id, profileImage, name)
    }
}
