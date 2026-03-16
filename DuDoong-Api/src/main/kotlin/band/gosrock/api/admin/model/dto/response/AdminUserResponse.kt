package band.gosrock.api.admin.model.dto.response

import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime

data class AdminUserResponse(
    val id: Long,
    val name: String?,
    val email: String?,
    val profileImage: String?,
    val accountRole: AccountRole,
    val accountState: AccountState,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(user: User): AdminUserResponse =
            AdminUserResponse(
                id = user.id!!,
                name = user.profile?.name,
                email = user.profile?.email,
                profileImage = user.profile?.profileImage?.imageKey,
                accountRole = user.accountRole,
                accountState = user.accountState,
                createdAt = user.createdAt,
            )
    }
}
