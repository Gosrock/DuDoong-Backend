package band.gosrock.infrastructure.outer.api.tossPayments.dto.response

import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsEnumNotMatchException
import com.fasterxml.jackson.annotation.JsonCreator

enum class FeeCode(val code: String, val kr: String) {
    BASE("BASE", "기본 수수료"),
    INSTALLMENT_DISCOUNT("INSTALLMENT_DISCOUNT", "PG사 부담 수수료"),
    INSTALLMENT("INSTALLMENT", "상점 부담 무이자 할부 수수료"),
    POINT_SAVING("POINT_SAVING", "카드사 포인트 적립 수수료"),
    ETC("ETC", "기본 수수료");

    companion object {
        @JsonCreator
        @JvmStatic
        fun findValue(code: String): FeeCode =
            values().firstOrNull { it.code == code }
                ?: throw PaymentsEnumNotMatchException.EXCEPTION
    }
}
