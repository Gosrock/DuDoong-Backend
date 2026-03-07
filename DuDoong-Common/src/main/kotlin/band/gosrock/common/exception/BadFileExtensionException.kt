package band.gosrock.common.exception

class BadFileExtensionException private constructor() : DuDoongCodeException(GlobalErrorCode.BAD_FILE_EXTENSION) {
    companion object {
@JvmField
        val EXCEPTION: DuDoongCodeException = BadFileExtensionException()
    }
}
