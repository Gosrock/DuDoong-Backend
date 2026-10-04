package band.gosrock.api.v2.host.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

data class V2TransferMasterRequest(
    @field:Schema(description = "새 마스터가 될 활성 멤버의 userId")
    @field:NotNull
    val userId: Long?,
)
