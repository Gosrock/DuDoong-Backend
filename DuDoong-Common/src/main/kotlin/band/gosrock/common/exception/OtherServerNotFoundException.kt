package band.gosrock.common.exception

class OtherServerNotFoundException private constructor() : DuDoongCodeException(GlobalErrorCode.OTHER_SERVER_NOT_FOUND) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = OtherServerNotFoundException()
    }
}
