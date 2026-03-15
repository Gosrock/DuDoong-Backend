package band.gosrock.api.host.model.dto.request

import band.gosrock.common.annotation.Enum
import band.gosrock.domain.domains.host.domain.HostRole
import io.swagger.v3.oas.annotations.media.Schema
import javax.validation.constraints.Positive

/** 호스트 유저 역할 수정 요청 DTO */
data class UpdateHostUserRoleRequest(
    @field:Schema(defaultValue = "1", description = "호스트 유저 아이디")
    @field:Positive(message = "올바른 유저 고유 아이디를 입력해주세요")
    val userId: Long,

    @field:Schema(defaultValue = "MANAGER", description = "호스트 유저 역할")
    @field:Enum(message = "GUEST, MANAGER, MASTER 만 허용됩니다")
    val role: HostRole,
)
