package band.gosrock.infrastructure.outer.api.tossPayments.config

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCreateErrorCode
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto
import feign.Response
import feign.codec.ErrorDecoder

class PaymentCreateErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        val body = TossPaymentsErrorDto.from(response)
        return try {
            val errorCode = PaymentsCreateErrorCode.valueOf(body.code ?: "")
            val errorReason = errorCode.getErrorReason()
            throw DuDoongDynamicException(errorReason.status, errorReason.code, errorReason.reason)
        } catch (e: IllegalArgumentException) {
            throw PaymentsUnHandleException.EXCEPTION
        }
    }
}
