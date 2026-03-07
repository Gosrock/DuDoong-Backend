package band.gosrock.common.exception

import band.gosrock.common.dto.ErrorReason

open class DuDoongCodeException(
    val errorCode: BaseErrorCode,
) : RuntimeException() {
    fun getErrorReason(): ErrorReason = errorCode.getErrorReason()
}
