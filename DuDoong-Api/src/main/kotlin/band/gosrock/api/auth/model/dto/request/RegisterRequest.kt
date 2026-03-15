package band.gosrock.api.auth.model.dto.request

import band.gosrock.domain.domains.user.domain.Profile
import javax.validation.constraints.NotEmpty

data class RegisterRequest(
    @field:NotEmpty
    val email: String? = null,
    val phoneNumber: String? = null,
    val profileImage: String? = null,
    @field:NotEmpty
    val name: String? = null,
    val marketingAgree: Boolean = false
) {
    fun toProfile(): Profile = Profile.builder()
        .profileImage(profileImage)
        .phoneNumber(phoneNumber)
        .name(name ?: "")
        .email(email ?: "")
        .build()
}
