package band.gosrock.api.v2.gift.dto

/** G-7 방향 */
enum class V2GiftDirection {
    SENT,
    RECEIVED,
}

/** T-1 정렬. 공연 임박순: 종료 전 공연(시작 임박순, 진행 중이 앞) → 지난 공연(최근 시작 순) */
enum class V2MyTicketSort {
    UPCOMING,
}

/**
 * 티켓탭 화면 상태 (02 문서 5-1, 11 문서 8-1). 판정 순서: 취소·환불 → 입장 완료 → 선물 완료 → 선물 대기(만료) → 받은 티켓 → 승인 완료.
 * [PENDING_APPROVE]·[REFUSED] 는 발급 티켓이 없는 주문 묶음에만 쓴다
 */
enum class V2MyTicketState {
    PENDING_APPROVE,
    REFUSED,
    APPROVED,
    GIFT_PENDING,
    GIFT_EXPIRED,
    GIFT_SENT,
    RECEIVED,
    ENTERED,
    CANCELED,
    REFUND_REQUESTED,
    REFUNDED,
}
