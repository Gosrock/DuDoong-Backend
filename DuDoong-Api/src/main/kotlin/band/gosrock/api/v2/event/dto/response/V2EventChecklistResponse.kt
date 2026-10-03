package band.gosrock.api.v2.event.dto.response

import band.gosrock.domain.domains.event.service.v2.EventChecklist
import io.swagger.v3.oas.annotations.media.Schema

data class V2EventChecklistResponse(
    @field:Schema(description = "기본 정보: 이름·일정·장소·문의처 1개 이상")
    val basic: Boolean,
    @field:Schema(description = "상세 정보: 본문이 있는 섹션 1개 이상 (섹션 없는 기존 공연은 v1 content)")
    val detail: Boolean,
    @field:Schema(description = "유효 티켓 1개 이상 존재 여부 (면제와 무관한 실제 상태)")
    val ticket: Boolean,
    @field:Schema(description = "티켓 항목 필요 여부. hasTicket=false 면 false (면제)")
    val ticketRequired: Boolean,
    @field:Schema(description = "등록 가능: 모든 항목 충족 + 시작 전 + 준비중")
    val canOpen: Boolean,
) {
    companion object {
        fun from(checklist: EventChecklist): V2EventChecklistResponse =
            V2EventChecklistResponse(
                basic = checklist.basic,
                detail = checklist.detail,
                ticket = checklist.ticket,
                ticketRequired = checklist.ticketRequired,
                canOpen = checklist.canOpen,
            )
    }
}
