package band.gosrock.api.v2.event.dto.response

import band.gosrock.domain.common.vo.EventSectionVo
import band.gosrock.domain.domains.event.domain.EventSectionContentFormat
import io.swagger.v3.oas.annotations.media.Schema

data class V2EventSectionResponse(
    @field:Schema(description = "섹션 id. 섹션이 없는 기존 공연의 v1 content 대체 표시면 null")
    val sectionId: Long?,
    val title: String,
    val content: String?,
    @field:Schema(description = "본문 형식. HTML(v2 에디터) / MARKDOWN(v1 본문). 프론트가 렌더러를 고른다")
    val contentFormat: EventSectionContentFormat,
    val sortOrder: Int,
) {
    companion object {
        fun from(vo: EventSectionVo): V2EventSectionResponse =
            V2EventSectionResponse(sectionId = vo.sectionId, title = vo.title, content = vo.content, contentFormat = vo.contentFormat, sortOrder = vo.sortOrder)
    }
}
