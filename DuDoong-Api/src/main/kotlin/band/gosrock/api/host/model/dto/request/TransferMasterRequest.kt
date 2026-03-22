package band.gosrock.api.host.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

/** 호스트 마스터 권한 양도 요청 DTO */
data class TransferMasterRequest(
    @field:Schema(description = "양도 대상 유저 아이디")
    @field:NotNull(message = "양도 대상 유저 아이디를 입력해주세요")
    val newMasterUserId: Long,
)
