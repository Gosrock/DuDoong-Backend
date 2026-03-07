package band.gosrock.common.exception

class InvalidTokenException private constructor() : DuDoongCodeException(GlobalErrorCode.INVALID_TOKEN) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = InvalidTokenException()
    }
}
