package band.gosrock.api.v2.host.dto.request

import band.gosrock.domain.domains.host.domain.Host
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class V2CreateHostRequest(
    @field:Schema(description = "호스트 이름 (1~15자)", example = "고스락")
    @field:NotBlank
    @field:Size(min = 1, max = 15)
    val name: String?,

    @field:Schema(description = "소개글")
    @field:Size(max = 255)
    val introduce: String? = null,

    @field:Schema(description = "대표 연락처 (1~${Host.MAX_CONTACT_COUNT}개)")
    @field:NotNull
    @field:Size(min = 1, max = Host.MAX_CONTACT_COUNT)
    @field:Valid
    val contacts: List<V2HostContactRequest>?,
)
