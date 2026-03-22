package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime

class UserInfoVo(
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
    }
}
