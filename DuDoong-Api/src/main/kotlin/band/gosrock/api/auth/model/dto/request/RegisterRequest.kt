package band.gosrock.api.auth.model.dto.request

import band.gosrock.domain.common.vo.ImageVo
import band.gosrock.domain.domains.user.domain.Profile
import jakarta.validation.constraints.NotEmpty

data class RegisterRequest(
    @field:NotEmpty
    val email: String? = null,
    val phoneNumber: String? = null,
    /** 카카오 프로필 이미지 주소만 저장한다 (#764). 형식이 다르면 가입은 그대로 하고 기본 이미지(null)로 둔다 — 카카오 CDN 호스트가 바뀌어도 가입이 막히지 않게 */
    val profileImage: String? = null,
    @field:NotEmpty
    val name: String? = null,
    val marketingAgree: Boolean = false
) {
    fun toProfile(): Profile = Profile(
        profileImage = profileImage?.takeIf { ImageVo.isKakaoImageUrl(it) },
        phoneNumber = phoneNumber,
        name = name ?: "",
        email = email ?: "",
    )
}
