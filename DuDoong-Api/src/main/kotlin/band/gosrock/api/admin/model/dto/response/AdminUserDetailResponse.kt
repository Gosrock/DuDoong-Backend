package band.gosrock.api.admin.model.dto.response

import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime

data class AdminUserDetailResponse(
    val id: Long,
    val name: String?,
    val email: String?,
    val profileImage: String?,
    val accountRole: AccountRole,
    val accountState: AccountState,
    val createdAt: LocalDateTime?,
    val phoneNumber: String?,
    val marketingAgree: Boolean,
    val oauthProvider: OauthProvider?,
) {
    companion object {
        fun from(user: User): AdminUserDetailResponse =
            AdminUserDetailResponse(
                id = user.id!!,
                name = user.profile?.name,
                email = user.profile?.email,
                profileImage = user.profile?.profileImage?.imageKey,
                accountRole = user.accountRole,
                accountState = user.accountState,
                createdAt = user.createdAt,
                phoneNumber = user.profile?.phoneNumberVo?.phoneNumber,
                marketingAgree = user.marketingAgree,
                oauthProvider = user.oauthInfo?.provider,
            )
    }
}
