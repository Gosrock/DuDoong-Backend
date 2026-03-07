package band.gosrock.common.exception

class OtherServerUnauthorizedException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_UNAUTHORIZED) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerUnauthorizedException()
    }
}
