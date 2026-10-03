package band.gosrock.api.v2.host.dto.request

import band.gosrock.domain.domains.host.domain.HostRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class V2AddHostMembersRequest(
    @field:Schema(description = "추가할 멤버 목록 (최대 ${MAX_MEMBERS}명)")
    @field:NotNull
    @field:Size(min = 1, max = MAX_MEMBERS)
    @field:Valid
    val members: List<V2AddHostMemberRequest>?,
) {
    companion object {
        const val MAX_MEMBERS = 20
    }
}

data class V2AddHostMemberRequest(
    @field:Schema(description = "가입된 유저 이메일", example = "member@gosrock.band")
    @field:NotBlank
    @field:Email
    val email: String?,

    @field:Schema(description = "역할. MANAGER 또는 GUEST (매니저 요청자는 GUEST 만)", example = "GUEST")
    @field:NotNull
    val role: HostRole?,
)
