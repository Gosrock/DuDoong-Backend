package band.gosrock.api.event.model.dto.response

import band.gosrock.domain.domains.event.domain.Event

/** 공연 체크 리스트 응답 DTO */
data class EventChecklistResponse(
    val hostId: Long?,
    val eventId: Long?,
    val name: String?,
    // 공연 기본 정보 (공연장 위치로 구분) 작성 여부
    val hasBasic: Boolean?,
    // 공연 상세 정보 작성 여부
    val hasDetail: Boolean?,
    // 티켓 상품 설정했는지 여부
    val hasTicketItem: Boolean?
) {
    companion object {
        @JvmStatic
        fun of(event: Event, hasTicket: Boolean): EventChecklistResponse {
            return EventChecklistResponse(
                hostId = event.hostId,
                eventId = event.id,
                name = event.eventBasic?.name,
                hasBasic = event.hasEventBasic() && event.hasEventPlace(),
                hasDetail = event.hasEventDetail(),
                hasTicketItem = hasTicket
            )
        }
    }
}
