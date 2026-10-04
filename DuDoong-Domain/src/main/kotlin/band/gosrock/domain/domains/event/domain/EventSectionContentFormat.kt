package band.gosrock.domain.domains.event.domain

/** 섹션 본문 형식. 프론트가 렌더러를 고른다 */
enum class EventSectionContentFormat {
    /** v2 리치 에디터 (서버 sanitize 후 저장) */
    HTML,

    /** v1 content (이관 / v1 수정 동기화) */
    MARKDOWN,
}
