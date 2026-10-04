package band.gosrock.domain.domains.gift.domain

/** 대기 중 선물이 CANCELED 가 된 이유 (#719) */
enum class TicketGiftCancelReason {
    /** 보낸 사람 회수 (G-2) */
    SENDER,

    /** 원 주문 취소 — 호스트 취소(v1·v2)·운영 취소 (DEC-026 #8) */
    ORDER_CANCELED,

    /** 보낸 사람 탈퇴·운영 정지 (DEC-026 #9·#10) */
    SENDER_WITHDRAWN,

    /** 공연 운영 삭제·비공개 전환 (DEC-026 #9) */
    EVENT_REMOVED,
}
