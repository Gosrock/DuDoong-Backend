package band.gosrock.api.v2.host.dto.request

import band.gosrock.domain.domains.host.domain.HostContact
import band.gosrock.domain.domains.host.domain.HostContactType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class V2HostContactRequest(
    @field:Schema(description = "연락처 유형", example = "INSTAGRAM")
    @field:NotNull
    val type: HostContactType?,

    @field:Schema(description = "연락처 값. PHONE 은 15자 이하", example = "@gosrock")
    @field:NotBlank
    @field:Size(max = HostContact.VALUE_MAX_LENGTH)
    val value: String?,
) {
    fun toEntity(): HostContact = HostContact(type = type!!, value = value!!.trim())
}
