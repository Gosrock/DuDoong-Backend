package band.gosrock.common.exception

class ExpiredTokenException private constructor() : DuDoongCodeException(GlobalErrorCode.TOKEN_EXPIRED) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = ExpiredTokenException()
    }
}
