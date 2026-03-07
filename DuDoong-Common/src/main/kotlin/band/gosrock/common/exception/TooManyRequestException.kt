package band.gosrock.common.exception

class TooManyRequestException private constructor() : DuDoongCodeException(GlobalErrorCode.TOO_MANY_REQUEST) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = TooManyRequestException()
    }
}
