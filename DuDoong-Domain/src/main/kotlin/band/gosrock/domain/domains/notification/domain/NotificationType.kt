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
}
