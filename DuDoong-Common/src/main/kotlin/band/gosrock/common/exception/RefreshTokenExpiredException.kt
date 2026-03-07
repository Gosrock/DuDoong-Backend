package band.gosrock.common.exception

class RefreshTokenExpiredException private constructor() : DuDoongCodeException(GlobalErrorCode.REFRESH_TOKEN_EXPIRED) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = RefreshTokenExpiredException()
    }
}
