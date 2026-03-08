package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException
import com.fasterxml.jackson.annotation.JsonCreator

enum class EasyPayCode(val kr: String, val en: String) {
    TOSSPAY("토스페이", "TOSSPAY"),
    NAVERPAY("네이버페이", "NAVERPAY"),
    SAMSUNGPAY("삼성페이", "SAMSUNGPAY"),
    LPAY("엘페이", "LPAY"),
    KAKAOPAY("카카오페이", "KAKAOPAY"),
    PAYCO("페이코", "PAYCO"),
    LGPAY("LG페이", "LGPAY"),
    SSG("SSG페이", "SSG");

    companion object {
        @JsonCreator
        @JvmStatic
        fun findValue(code: String): EasyPayCode =
            values().firstOrNull { it.kr == code }
                ?: throw PaymentsEnumNotMatchException.EXCEPTION
    }
}
