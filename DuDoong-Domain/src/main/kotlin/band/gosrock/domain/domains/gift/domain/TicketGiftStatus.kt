package band.gosrock.domain.domains.gift.domain

/** 선물 상태 (#719, 11 문서 8-2 전이표). 시간 만료 상태는 없다 — 공연 종료 뒤 PENDING 은 조회 시 '선물 만료'로 판정 (DEC-026 #7) */
enum class TicketGiftStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    RETURNED,
    CANCELED,
}
