package band.gosrock.api.v2.event.dto.response

import band.gosrock.domain.common.vo.EventSectionVo
import io.swagger.v3.oas.annotations.media.Schema

data class V2EventSectionResponse(
    @field:Schema(description = "섹션 id. 섹션이 없는 기존 공연의 v1 content 대체 표시면 null")
    val sectionId: Long?,
    val title: String,
    val content: String?,
    val sortOrder: Int,
) {
    companion object {
        fun from(vo: EventSectionVo): V2EventSectionResponse =
            V2EventSectionResponse(sectionId = vo.sectionId, title = vo.title, content = vo.content, sortOrder = vo.sortOrder)
    }
}
