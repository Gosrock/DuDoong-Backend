package band.gosrock.api.v2.host.dto.response

import band.gosrock.domain.domains.host.domain.Host
import io.swagger.v3.oas.annotations.media.Schema

data class V2MyHostResponse(
    val hostId: Long,
    val name: String?,
    val profileImageUrl: String?,
    @field:Schema(description = "내 역할 (MASTER / MANAGER / GUEST)", allowableValues = ["MASTER", "MANAGER", "GUEST"])
    val myRole: String,
    @field:Schema(description = "등록된 공연 수 (삭제 제외)")
    val eventCount: Long,
) {
    companion object {
        fun of(host: Host, userId: Long, eventCount: Long): V2MyHostResponse =
            V2MyHostResponse(
                hostId = host.id!!,
                name = host.profile?.name,
                profileImageUrl = host.profile?.profileImage?.generateImageUrl(),
                myRole = host.getHostUserByUserId(userId).role.name,
                eventCount = eventCount,
            )
    }
}
