package band.gosrock.api.v2.event.dto.response

import band.gosrock.domain.domains.event.service.v2.EventChecklist
import io.swagger.v3.oas.annotations.media.Schema

/** E-7 체크리스트 (E-3 checklist 도 같음). #755: basic → isBasicFilled, detail → isDetailFilled, ticket → hasValidTicket (Boolean 접두 규칙) */
data class V2EventChecklistResponse(
    @field:Schema(description = "기본 정보: 포스터·이름·일정(시작/종료)·장소(이름·주소·좌표)·문의처 1개 이상")
    val isBasicFilled: Boolean,
    @field:Schema(description = "상세 정보: 본문이 있는 섹션 1개 이상 (섹션 없는 기존 공연은 v1 content)")
    val isDetailFilled: Boolean,
    @field:Schema(description = "유효 티켓 1개 이상 존재 여부 (면제와 무관한 실제 상태)")
    val hasValidTicket: Boolean,
    @field:Schema(description = "티켓 항목 필요 여부. hasTicket=false 면 false (면제)")
    val ticketRequired: Boolean,
    @field:Schema(description = "등록 가능: 모든 항목 충족 + 시작 전 + 준비중")
    val canOpen: Boolean,
) {
    companion object {
        fun from(checklist: EventChecklist): V2EventChecklistResponse =
            V2EventChecklistResponse(
                isBasicFilled = checklist.basic,
                isDetailFilled = checklist.detail,
                hasValidTicket = checklist.ticket,
                ticketRequired = checklist.ticketRequired,
                canOpen = checklist.canOpen,
            )
    }
}
