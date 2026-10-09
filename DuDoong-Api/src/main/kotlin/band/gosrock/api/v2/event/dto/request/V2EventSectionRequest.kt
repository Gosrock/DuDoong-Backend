package band.gosrock.api.v2.event.dto.request

import band.gosrock.domain.domains.event.domain.EventSection
import io.swagger.v3.oas.annotations.media.Schema

/**
 * 섹션 하나. 요청 본문은 `{sections: [{title, content, sortOrder}]}`(#755: 맨 배열 → 객체) 이고 전체 교체한다.
 * 개수(1~10)·제목(1~20자)·본문 길이 검증은 도메인에서 한다 (Event_400_21).
 */
data class V2EventSectionRequest(
    @field:Schema(description = "섹션 제목 (1~${EventSection.TITLE_MAX_LENGTH}자). 첫 섹션('공연 소개') 본문은 v1 상세에도 보인다", example = "공연 소개")
    val title: String?,

    @field:Schema(description = "본문 (리치 에디터 HTML, ${EventSection.CONTENT_MAX_LENGTH}자 이하). 서버에서 sanitize 해 HTML 형식으로 저장")
    val content: String?,

    @field:Schema(description = "정렬 순서. 오름차순으로 정렬해 0부터 다시 매긴다", example = "0")
    val sortOrder: Int?,
)

/** E-6 섹션 전체 저장 본문 (#755: 나중에 필드를 더할 수 있게 객체로 감싼다 — O-5 `{optionIds}` 와 같은 모양) */
data class V2UpdateEventSectionsRequest(
    @field:Schema(description = "섹션 전체 (1~10개, 배열 순서가 아니라 sortOrder 순). 빠진 섹션은 지워진다. 없거나 빈 배열·null 원소는 Event_400_21")
    val sections: List<V2EventSectionRequest?>? = null,
)
