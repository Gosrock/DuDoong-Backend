package band.gosrock.api.host.model.dto.response

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.User
import io.swagger.v3.oas.annotations.media.Schema

/** 초대할 유저 검색 결과. 검색한 이메일 외의 개인정보는 담지 않는다 */
data class InviteUserResponse(
    @field:Schema(description = "유저 고유 아이디")
    val userId: Long?,

    @field:Schema(description = "이름")
    val userName: String?,

    @field:Schema(description = "프로필 이미지")
    val profileImage: ImageVo?,
) {
    companion object {
        fun from(user: User): InviteUserResponse =
            InviteUserResponse(
                userId = user.id,
                userName = user.profile?.name,
                profileImage = user.profile?.profileImage,
            )
    }
}
