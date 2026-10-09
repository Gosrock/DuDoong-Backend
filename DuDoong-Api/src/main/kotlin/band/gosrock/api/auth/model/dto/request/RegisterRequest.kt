package band.gosrock.api.auth.model.dto.request

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.Profile
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class RegisterRequest(
    @field:NotEmpty
    val email: String? = null,
    val phoneNumber: String? = null,
    /** 카카오 프로필 이미지 주소만 받는다 (#764). 없으면 null·빈 문자열(기존 동작 유지) */
    @field:Pattern(regexp = "^$|" + ImageVo.KAKAO_IMAGE_URL_PATTERN, message = "카카오 프로필 이미지 주소만 사용할 수 있습니다.")
    val profileImage: String? = null,
    @field:NotEmpty
    val name: String? = null,
    val marketingAgree: Boolean = false
) {
    fun toProfile(): Profile = Profile(
        profileImage = profileImage,
        phoneNumber = phoneNumber,
        name = name ?: "",
        email = email ?: "",
    )
}
