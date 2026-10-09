package band.gosrock.api.v2.host.dto.response

import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema

data class V2HostMemberResponse(
    val userId: Long,
    val name: String?,
    val profileImageUrl: String?,
    @field:Schema(description = "역할 (MASTER / MANAGER / GUEST)", allowableValues = ["MASTER", "MANAGER", "GUEST"])
    val role: String,
) {
    companion object {
        fun of(hostUser: HostUser, user: User?): V2HostMemberResponse =
            V2HostMemberResponse(
                userId = hostUser.userId!!,
                name = user?.profile?.name,
                profileImageUrl = user?.profile?.profileImage?.generateImageUrl(),
                role = hostUser.role.name,
            )
    }
}
