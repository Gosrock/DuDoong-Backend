package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema

/** 호스트 상세의 멤버 정보. 연락처·마케팅 동의 등 개인정보는 담지 않는다 */
data class HostMemberResponse(
    @field:Schema(description = "유저 고유 아이디")
    val userId: Long?,

    @field:Schema(description = "이름")
    val userName: String?,

    @field:Schema(description = "프로필 이미지")
    val profileImage: ImageVo?,

    @field:Schema(description = "호스트에서의 역할")
    val role: HostRole?,

    @field:Schema(description = "초대를 수락했는지 여부")
    val active: Boolean?,
) {
    companion object {
        fun of(user: User, hostUser: HostUser): HostMemberResponse =
            HostMemberResponse(
                userId = user.id,
                userName = user.profile?.name,
                profileImage = user.profile?.profileImage,
                role = hostUser.role,
                active = hostUser.active,
            )
    }
}
