package band.gosrock.infrastructure.outer.api.tossPayments.config

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCancelErrorCode
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto
import feign.FeignException
import feign.Response
import feign.RetryableException
import feign.Retryer
import feign.codec.ErrorDecoder
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpStatus
import java.util.concurrent.TimeUnit

class PaymentCancelErrorDecoder : ErrorDecoder {

    @Bean
    fun retryer(): Retryer.Default = Retryer.Default(PERIOD, MAX_PERIOD, MAX_ATTEMPTS)

    override fun decode(methodKey: String, response: Response): Exception {
        return try {
            val exception = FeignException.errorStatus(methodKey, response)
            val status = response.status()
            if (HttpStatus.valueOf(status).is5xxServerError) {
                throw RetryableException(
                    status,
                    exception.message,
                    response.request().httpMethod(),
                    exception,
                    null as Long?,
                    response.request(),
                )
            }
            val body = TossPaymentsErrorDto.from(response)
            val errorCode = PaymentsCancelErrorCode.valueOf(body.code ?: "")
            val errorReason = errorCode.getErrorReason()
            throw DuDoongDynamicException(errorReason.status, errorReason.code, errorReason.reason)
        } catch (e: IllegalArgumentException) {
            throw PaymentsUnHandleException.EXCEPTION
        }
    }

    companion object {
        private const val PERIOD = 500L
        private val MAX_PERIOD = TimeUnit.SECONDS.toMillis(3L)
        private const val MAX_ATTEMPTS = 3
    }
}
