package band.gosrock.common.exception

class OauthStateMismatchException private constructor() : DuDoongCodeException(GlobalErrorCode.OAUTH_STATE_MISMATCH) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OauthStateMismatchException()
    }
}
