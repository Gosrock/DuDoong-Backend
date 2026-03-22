package band.gosrock.admin.model.dto.request

import jakarta.validation.constraints.NotNull

/** 어드민 호스트 마스터 강제 양도 요청 DTO */
data class AdminTransferMasterRequest(
    @field:NotNull(message = "양도 대상 유저 아이디를 입력해주세요")
    val newMasterUserId: Long,
)
