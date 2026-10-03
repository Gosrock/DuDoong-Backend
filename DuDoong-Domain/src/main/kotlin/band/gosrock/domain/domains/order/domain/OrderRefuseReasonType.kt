package band.gosrock.domain.domains.order.domain

/**
 * 주문 거절 사유 종류 (v2, #712). `tbl_order.refuse_reason_type` 에 저장한다.
 * v1 화면 호환을 위해 표시 문구([label], 기타는 직접 입력값)는 기존 `cancel_reason` 에도 기록한다.
 * v1 API 로 거절한 주문은 이 값이 null 이다.
 */
enum class OrderRefuseReasonType(val label: String) {
    DEPOSIT_UNCONFIRMED("입금 미확인"),
    AMOUNT_MISMATCH("결제 금액 오류"),
    SOLD_OUT("티켓 매진"),
    ETC("기타"),
}
