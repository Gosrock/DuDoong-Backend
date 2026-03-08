package band.gosrock.infrastructure.outer.api.tossPayments.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus.BAD_REQUEST

enum class PaymentsCreateErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    NOT_SUPPORTED_METHOD(BAD_REQUEST.value(), "PAYMENTS_CREATE_NOT_SUPPORTED_METHOD", "지원되지 않는 결제 수단입니다."),
    INVALID_SUCCESS_URL(BAD_REQUEST.value(), "PAYMENTS_CREATE_INVALID_SUCCESS_URL", "`successUrl`은 필수 파라미터입니다."),
    DUPLICATED_ORDER_ID(BAD_REQUEST.value(), "PAYMENTS_CREATE_DUPLICATED_ORDER_ID", "이미 승인 및 취소가 진행된 중복된 주문번호 입니다. 다른 주문번호로 진행해주세요."),
    INVALID_REQUEST(BAD_REQUEST.value(), "PAYMENTS_CREATE_INVALID_REQUEST", "잘못된 요청입니다.");

    override fun getErrorReason(): ErrorReason = ErrorReason(status, code, reason)

    @Throws(NoSuchFieldException::class)
    override fun getExplainError(): String {
        val field = this::class.java.getField(this.name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return if (annotation != null) annotation.value else reason
    }
}
