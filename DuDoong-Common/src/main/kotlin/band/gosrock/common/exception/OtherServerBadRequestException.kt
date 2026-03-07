package band.gosrock.common.exception

class OtherServerBadRequestException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_BAD_REQUEST) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerBadRequestException()
    }
}
