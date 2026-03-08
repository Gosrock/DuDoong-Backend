package band.gosrock.infrastructure.outer.api.tossPayments.config

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TransactionGetErrorCode
import feign.Response
import feign.codec.ErrorDecoder

class TransactionGetErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        val body = TossPaymentsErrorDto.from(response)
        return try {
            val errorCode = TransactionGetErrorCode.valueOf(body.code ?: "")
            val errorReason = errorCode.getErrorReason()
            throw DuDoongDynamicException(errorReason.status, errorReason.code, errorReason.reason)
        } catch (e: IllegalArgumentException) {
            throw PaymentsUnHandleException.EXCEPTION
        }
    }
}
