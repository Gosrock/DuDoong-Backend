package band.gosrock.common.exception

class BadLockIdentifierException private constructor() : DuDoongCodeException(GlobalErrorCode.BAD_LOCK_IDENTIFIER) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = BadLockIdentifierException()
    }
}
