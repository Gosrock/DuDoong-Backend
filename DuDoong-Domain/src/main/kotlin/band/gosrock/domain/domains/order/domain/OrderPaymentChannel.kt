package band.gosrock.domain.domains.order.domain

/**
 * v2 사용자 주문의 결제 방식 (#718, `tbl_order.payment_channel`). v1 주문은 null.
 * 두 계좌송금 방식은 백엔드에서 구분해 기록만 한다 (DEC-022 — 토스 송금 딥링크는 프론트가 만든다, PG 연동 없음)
 */
enum class OrderPaymentChannel {
    /** 직접 계좌이체 (두둥티켓) */
    BANK_TRANSFER,

    /** 토스 간편송금 링크로 송금 (두둥티켓) */
    TOSS_TRANSFER,

    /** 무료 티켓 */
    FREE;

    fun isTransfer(): Boolean = this == BANK_TRANSFER || this == TOSS_TRANSFER
}
