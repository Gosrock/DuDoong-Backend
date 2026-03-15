package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import io.swagger.v3.oas.annotations.media.Schema

/** 내가 속한 호스트의 간략한 정보에 대한 응답 DTO */
data class HostProfileResponse(
    @field:Schema(description = "호스트 고유 아이디")
    val hostId: Long?,

    @field:Schema(description = "호스트 이름")
    val name: String?,

    @field:Schema(description = "호스트 한줄 소개")
    val introduce: String?,

    @field:Schema(description = "호스트 프로필 이미지")
    val profileImage: ImageVo?,

    @field:Schema(description = "속한 호스트에서의 역할")
    val role: HostRole,

    @field:Schema(description = "이 호스트의 마스터인지 여부")
    val isMaster: Boolean,

    @field:Schema(description = "이 호스트 초대를 수락했는지 여부")
    val active: Boolean?,
) {
    companion object {
        fun of(host: Host, userId: Long): HostProfileResponse {
            val hostUser = host.getHostUserByUserId(userId)
            return HostProfileResponse(
                hostId = host.id,
                name = host.profile!!.name,
                introduce = host.profile!!.introduce,
                profileImage = host.profile!!.profileImage,
                role = hostUser.role,
                isMaster = host.masterUserId == userId,
                active = hostUser.active,
            )
        }
    }
}
