package band.gosrock.common.exception

class SecurityContextNotFoundException private constructor() : DuDoongCodeException(GlobalErrorCode.SECURITY_CONTEXT_NOT_FOUND) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = SecurityContextNotFoundException()
    }
}
