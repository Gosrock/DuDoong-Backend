package band.gosrock.domain.domains.order.domain

import com.fasterxml.jackson.annotation.JsonValue

enum class OrderMethod(
    val value: String,
    @JsonValue val kr: String,
) {
    APPROVAL("APPROVAL", "승인 방식"),
    PAYMENT("PAYMENT", "결제 방식");

    fun isPayment(): Boolean = this == PAYMENT
}
