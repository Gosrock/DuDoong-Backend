package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime

class UserInfoVo private constructor(
    val userId: Long?,
    val userName: String?,
    val email: String?,
    val phoneNumber: PhoneNumberVo?,
    val profileImage: ImageVo?,
    val createdAt: LocalDateTime?,
    var receiveMail: Boolean?,
    var marketingAgree: Boolean?,
) {
    companion object {
        @JvmStatic
        fun from(user: User): UserInfoVo =
            UserInfoVo(
                userId = user.id,
                userName = user.profile?.name,
                email = user.profile?.email,
                profileImage = user.profile?.profileImage,
                phoneNumber = user.profile?.phoneNumberVo,
                createdAt = user.createdAtKt(),
                receiveMail = user.isReceiveEmail(),
                marketingAgree = user.isAgreeMarketing(),
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var userName: String? = null
        private var email: String? = null
        private var phoneNumber: PhoneNumberVo? = null
        private var profileImage: ImageVo? = null
        private var createdAt: LocalDateTime? = null
        private var receiveMail: Boolean? = null
        private var marketingAgree: Boolean? = null

        fun userId(v: Long?) = apply { userId = v }
        fun userName(v: String?) = apply { userName = v }
        fun email(v: String?) = apply { email = v }
        fun phoneNumber(v: PhoneNumberVo?) = apply { phoneNumber = v }
        fun profileImage(v: ImageVo?) = apply { profileImage = v }
        fun createdAt(v: LocalDateTime?) = apply { createdAt = v }
        fun receiveMail(v: Boolean?) = apply { receiveMail = v }
        fun marketingAgree(v: Boolean?) = apply { marketingAgree = v }
        fun build() = UserInfoVo(userId, userName, email, phoneNumber, profileImage, createdAt, receiveMail, marketingAgree)
    }
}
