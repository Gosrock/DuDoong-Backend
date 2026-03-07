package band.gosrock.common.exception

class DuDoongDynamicException(
    val status: Int,
    val code: String,
    val reason: String,
) : RuntimeException()
