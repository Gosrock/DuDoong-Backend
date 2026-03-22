package band.gosrock.domain.domains.order.domain

enum class RefundStatus(val value: String, val description: String) {
    NONE("NONE", "환불 불필요"),
    REFUND_REQUESTED("REFUND_REQUESTED", "환불 요청됨"),
    REFUND_COMPLETED("REFUND_COMPLETED", "환불 완료"),
}
