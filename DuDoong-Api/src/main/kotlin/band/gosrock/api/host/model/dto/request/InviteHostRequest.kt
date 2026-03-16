package band.gosrock.api.host.model.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.host.domain.HostRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Email

/** 호스트 초대 요청 DTO */
data class InviteHostRequest(
    @field:Schema(defaultValue = "kls1998@naver.com", description = "초대할 유저 이메일 주소")
    @field:Email(message = "올바른 이메일을 입력해주세요")
    val email: String,

    @field:Schema(defaultValue = "MANAGER", description = "호스트 유저 역할")
    @field:Enum(message = "MANAGER 또는 GUEST 만 허용됩니다")
    val role: HostRole,
)
