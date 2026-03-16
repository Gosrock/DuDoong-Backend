package band.gosrock.api.host.model.dto.request

import band.gosrock.common.annotation.Phone
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

/** 호스트 간편 생성 요청 DTO */
data class CreateHostRequest(
    @field:Schema(defaultValue = "고스락", description = "호스트 이름")
    @field:NotBlank(message = "호스트 이름을 입력해주세요")
    @field:Length(max = 15)
    val name: String,

    @field:Schema(defaultValue = "gosrock@gsrk.com", description = "마스터 이메일")
    @field:Email(message = "올바른 형식의 이메일을 입력하세요")
    val contactEmail: String,

    @field:Schema(defaultValue = "010-1111-3333", description = "마스터 전화번호")
    @field:Phone(message = "올바른 형식의 번호를 입력하세요")
    val contactNumber: String,
)
