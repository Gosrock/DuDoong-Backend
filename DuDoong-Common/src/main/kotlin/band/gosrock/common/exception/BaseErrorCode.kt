package band.gosrock.common.exception

import band.gosrock.common.dto.ErrorReason

interface BaseErrorCode {
    fun getErrorReason(): ErrorReason

    @Throws(NoSuchFieldException::class)
    fun getExplainError(): String
}
