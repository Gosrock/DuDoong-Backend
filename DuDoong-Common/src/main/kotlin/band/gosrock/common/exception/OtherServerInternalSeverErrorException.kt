package band.gosrock.common.exception

class OtherServerInternalSeverErrorException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_INTERNAL_SERVER_ERROR) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerInternalSeverErrorException()
    }
}
