package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException
import com.fasterxml.jackson.annotation.JsonCreator

enum class TossPaymentMethod(val kr: String) {
    CARD("카드"),
    VIRTUAL_ACCOUNT("가상계좌"),
    EASYPAY("간편결제"),
    MOBILE_PAY("휴대폰"),
    BANK_TRANSFER("계좌이체"),
    GIFT_CARD("상품권");

    companion object {
        @JsonCreator
        @JvmStatic
        fun findValue(code: String): TossPaymentMethod =
            values().firstOrNull { it.kr == code }
                ?: throw PaymentsEnumNotMatchException.EXCEPTION
    }
}
