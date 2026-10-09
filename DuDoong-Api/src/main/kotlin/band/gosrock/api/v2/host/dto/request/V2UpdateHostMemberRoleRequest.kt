package band.gosrock.api.v2.host.dto.request

import band.gosrock.api.v2.host.dto.V2HostMemberRole
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class V2UpdateHostMemberRoleRequest(
    @field:Schema(description = "변경할 역할. MANAGER 또는 GUEST", example = "MANAGER")
    @field:NotNull
    val role: V2HostMemberRole?,
)
