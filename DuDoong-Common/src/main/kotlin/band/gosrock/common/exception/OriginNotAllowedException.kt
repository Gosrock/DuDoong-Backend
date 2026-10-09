package band.gosrock.common.exception

class OriginNotAllowedException private constructor() : DuDoongCodeException(GlobalErrorCode.ORIGIN_NOT_ALLOWED) {
    companion object {
        @JvmField
        val EXCEPTION: DuDoongCodeException = OriginNotAllowedException()
    }
}
