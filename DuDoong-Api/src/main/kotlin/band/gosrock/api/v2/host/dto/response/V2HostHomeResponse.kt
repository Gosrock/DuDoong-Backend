package band.gosrock.api.v2.host.dto.response

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.host.domain.Host
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/** 공개 호스트 홈. 멤버 목록·슬랙 url 등 내부 정보는 노출하지 않는다 */
data class V2HostHomeResponse(
    val hostId: Long,
    val name: String?,
    val introduce: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    @field:Schema(description = "대표 연락처. v2 연락처가 없으면 v1 연락처(전화/이메일)로 대체")
    val contacts: List<HostContactVo>,
    @field:Schema(description = "개설일")
    @field:DateFormat
    val createdAt: LocalDateTime?,
    @field:Schema(description = "활성 멤버 수")
    val memberCount: Int,
    val followerCount: Long,
    @field:Schema(description = "내가 팔로우 중인지 (비로그인 false)")
    val isFollowing: Boolean,
    @field:Schema(description = "내 역할 (MASTER / MANAGER / GUEST). 활성 멤버가 아니면 null")
    val myRole: String?,
) {
    companion object {
        fun of(host: Host, followerCount: Long, isFollowing: Boolean, userId: Long): V2HostHomeResponse =
            V2HostHomeResponse(
                hostId = host.id!!,
                name = host.profile?.name,
                introduce = host.profile?.introduce,
                profileImageUrl = host.profile?.profileImage?.generateImageUrl(),
                coverImageUrl = host.profile?.coverImage?.generateImageUrl(),
                contacts = host.displayContacts(),
                createdAt = host.createdAt,
                memberCount = host.getActiveHostUsers().size,
                followerCount = followerCount,
                isFollowing = isFollowing,
                myRole = host.getActiveRoleOf(userId)?.name,
            )
    }
}
