package band.gosrock.infrastructure.outer.api.tossPayments.exception

import band.gosrock.common.annotation.ExplainError
import band.gosrock.common.dto.ErrorReason
import band.gosrock.common.exception.BaseErrorCode
import org.springframework.http.HttpStatus.BAD_REQUEST

enum class TransactionGetErrorCode(
    private val status: Int,
    private val code: String,
    private val reason: String,
) : BaseErrorCode {
    UNAUTHORIZED_KEY(BAD_REQUEST.value(), "PAYMENTS_GET_UNAUTHORIZED_KEY", "인증되지 않은 시크릿 키 혹은 클라이언트 키 입니다."),
    FORBIDDEN_CONSECUTIVE_REQUEST(BAD_REQUEST.value(), "PAYMENTS_GET_FORBIDDEN_CONSECUTIVE_REQUEST", "반복적인 요청은 허용되지 않습니다. 잠시 후 다시 시도해주세요."),
    NOT_FOUND_PAYMENT(BAD_REQUEST.value(), "PAYMENTS_GET_NOT_FOUND_PAYMENT", "존재하지 않는 결제 정보 입니다."),
    NOT_FOUND(BAD_REQUEST.value(), "PAYMENTS_GET_NOT_FOUND", "존재하지 않는 정보 입니다."),
    FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING(BAD_REQUEST.value(), "PAYMENTS_GET_FAILED_PAYMENT_INTERNAL_SYSTEM_PROCESSING", "결제가 완료되지 않았어요. 다시 시도해주세요.");

    override fun getErrorReason(): ErrorReason = ErrorReason(status, code, reason)

    @Throws(NoSuchFieldException::class)
    override fun getExplainError(): String {
        val field = this::class.java.getField(this.name)
        val annotation = field.getAnnotation(ExplainError::class.java)
        return if (annotation != null) annotation.value else reason
    }
}
