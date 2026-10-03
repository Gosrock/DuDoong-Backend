package band.gosrock.api.v2.event.dto.request

import band.gosrock.domain.domains.event.domain.EventSection
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 섹션 하나. 요청 본문은 배열 `[{title, content, sortOrder}]` 이고 전체 교체한다.
 * 개수(1~10)·제목(1~20자)·본문 길이 검증은 도메인에서 한다 (Event_400_21).
 */
data class V2EventSectionRequest(
    @field:Schema(description = "섹션 제목 (1~${EventSection.TITLE_MAX_LENGTH}자). '공연 소개' 섹션 본문은 v1 상세에도 보인다", example = "공연 소개")
    val title: String?,

    @field:Schema(description = "본문 (리치 에디터 HTML, ${EventSection.CONTENT_MAX_LENGTH}자 이하)")
    val content: String?,

    @field:Schema(description = "정렬 순서. 오름차순으로 정렬해 0부터 다시 매긴다", example = "0")
    val sortOrder: Int?,
)
