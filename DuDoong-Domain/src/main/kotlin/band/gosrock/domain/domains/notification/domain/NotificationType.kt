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

    /** 승인 완료 주문을 호스트가 취소 (v1/v2, 승인형). 수신자: 주문자 (#726) */
    ORDER_CANCELED_BY_HOST,

    /** 환불 완료 — 호스트가 송금 후 환불 완료 처리 (v1/v2, 돌려준 돈이 있는 주문만, 카드(PG) 결제 제외). 수신자: 주문자 (#726) */
    ORDER_REFUND_COMPLETED,

    /** 사용자가 이미 입력한 환불 계좌를 바꿈 (v2 O-5, 첫 입력 제외). 수신자: 호스트 활성 마스터·매니저 (#728) */
    REFUND_ACCOUNT_CHANGED,

    // ===== 선물 (#719, 11 문서 8-2 알림 표 — 보낸 사람 회수는 알림 없음) =====

    /** 선물 링크 생성 (G-1). 수신자: 보낸 사람 */
    GIFT_SENT,

    /** 선물 수락 (G-4). 수신자: 보낸 사람 */
    GIFT_ACCEPTED,

    /** 선물 수락 (G-4). 수신자: 받은 사람 */
    GIFT_RECEIVED,

    /** 선물 거절 (G-5). 수신자: 보낸 사람 */
    GIFT_REJECTED,

    /** 수락 후 반환 (G-6). 수신자: 보낸 사람 */
    GIFT_RETURNED,

    /** 선물받은 티켓이 원 주문 취소(호스트·운영 취소)로 취소됨 (DEC-026 #8). 수신자: 받은 사람 */
    GIFT_TICKET_CANCELED,
}
