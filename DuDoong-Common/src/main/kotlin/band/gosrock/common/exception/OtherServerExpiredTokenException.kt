package band.gosrock.common.exception

class OtherServerExpiredTokenException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_EXPIRED_TOKEN) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerExpiredTokenException()
    }
}
