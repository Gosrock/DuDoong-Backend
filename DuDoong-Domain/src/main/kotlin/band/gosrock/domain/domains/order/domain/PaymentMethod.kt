package band.gosrock.domain.domains.order.domain

import band.gosrock.domain.domains.order.exception.NotSupportedOrderMethodException
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.TossPaymentMethod
import com.fasterxml.jackson.annotation.JsonValue

enum class PaymentMethod(
    val value: String,
    @JsonValue val kr: String,
) {
    EASYPAY("EASYPAY", "간편 결제"),
    CARD("CARD", "카드 결제"),
    DEFAULT("DEFAULT", "");

    companion object {
        @JvmStatic
        fun from(tossPaymentMethod: TossPaymentMethod): PaymentMethod =
            try {
                valueOf(tossPaymentMethod.name)
            } catch (e: IllegalArgumentException) {
                throw NotSupportedOrderMethodException.EXCEPTION
            }
    }
}
