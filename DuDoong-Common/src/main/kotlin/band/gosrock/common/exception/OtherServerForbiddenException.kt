package band.gosrock.common.exception

class OtherServerForbiddenException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_FORBIDDEN) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerForbiddenException()
    }
}
