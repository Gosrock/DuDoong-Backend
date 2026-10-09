package band.gosrock.api.v2.common

/**
 * v2 목록 파라미터 규칙 (#755 C16·C38): 페이지 크기, 검색어 길이. `page` 는 0부터. 값은 컨트롤러마다 따로 두지 않고 여기서 쓴다.
 * - 기본 크기: 카드형 목록(E-1·H-1·H-14·P-2) [CARD_DEFAULT_SIZE], 그 밖 [DEFAULT_SIZE]
 * - 상한: 일반 [MAX_SIZE], 운영 표·알림(R-1·F-1·I-1·N-1) [TABLE_MAX_SIZE]
 */
object V2Paging {
    const val DEFAULT_SIZE = "20"
    const val CARD_DEFAULT_SIZE = "10"
    const val MAX_SIZE = 50L
    const val TABLE_MAX_SIZE = 100L

    /** 검색어(`keyword`) 최대 길이 — 모든 v2 목록 검색 공통 (#755 C38) */
    const val KEYWORD_MAX_LENGTH = 50
}
