package band.gosrock.domain.domains.notification.domain

/** 알림 종류 (v2 알림센터 1차, #714) */
enum class NotificationType {
    /** 호스트 멤버로 추가됨 (v2 멤버 추가, 수락 없음 — DEC-015). 수신자: 추가된 사용자 */
    HOST_MEMBER_ADDED,

    /** 승인형 주문 접수. 수신자: 호스트 활성 마스터·매니저 */
    ORDER_PENDING_APPROVE,

    /** 승인형 주문 승인. 수신자: 주문자 */
    ORDER_APPROVED,

    /** 승인형 주문 거절 (v1/v2). 수신자: 주문자 */
    ORDER_REFUSED,

    /** 사용자 취소·환불 요청 — 돌려줄 돈이 있음(REFUND + 환불 요청, v1/v2). 수신자: 호스트 활성 마스터·매니저 (#718) */
    ORDER_REFUND_REQUESTED,

    /** 사용자 취소 — 돌려줄 돈 없음(v2 무료 주문 취소, REFUND + 환불 NONE). 수신자: 호스트 활성 마스터·매니저 (#718) */
    ORDER_CANCELED_BY_USER,
}
