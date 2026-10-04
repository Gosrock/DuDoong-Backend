package band.gosrock.domain.common.vo

import band.gosrock.domain.domains.event.domain.EventSectionContentFormat

/** v2 섹션 표시용. [sectionId] 가 null 이면 섹션이 없는 기존 공연의 v1 content 대체 표시 */
data class EventSectionVo(
    val sectionId: Long?,
    val title: String,
    val content: String?,
    val contentFormat: EventSectionContentFormat,
    val sortOrder: Int,
)
