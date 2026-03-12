package band.gosrock.domain.domains.order.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class OrderStatus(
    val value: String,
    @JsonValue val kr: String,
) {
    READY("READY", "주문 생성상태"),
    PENDING_PAYMENT("PENDING_PAYMENT", "결제 대기중"),
    PENDING_APPROVE("PENDING_APPROVE", "승인 대기중"),
    OUTDATED("OUTDATED", "결제 시간 만료"),
    CONFIRM("CONFIRM", "결제 완료"),
    APPROVED("APPROVED", "승인 완료"),
    REFUND("REFUND", "환불 완료"),
    CANCELED("CANCELED", "취소된 결제"),
    FAILED("FAILED", "결제 실패");

    fun isInEventOrderExcelStatus(): Boolean =
        this == CONFIRM || this == CANCELED || this == APPROVED || this == REFUND

    fun isCanDone(): Boolean =
        this == PENDING_PAYMENT || this == PENDING_APPROVE

    fun isCanWithDraw(): Boolean =
        this == APPROVED || this == CONFIRM
}
