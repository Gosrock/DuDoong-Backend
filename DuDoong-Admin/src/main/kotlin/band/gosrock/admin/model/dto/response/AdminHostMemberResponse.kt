package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import java.time.LocalDateTime

data class AdminHostMemberResponse(
    val userId: Long?,
    val userName: String?,
    val role: HostRole,
    val active: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(hostUser: HostUser, userName: String?): AdminHostMemberResponse =
            AdminHostMemberResponse(
                userId = hostUser.userId,
                userName = userName,
                role = hostUser.role,
                active = hostUser.active,
                createdAt = hostUser.createdAt,
            )
    }
}
