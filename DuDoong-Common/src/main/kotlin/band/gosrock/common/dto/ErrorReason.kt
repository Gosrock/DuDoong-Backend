package band.gosrock.common.dto

data class ErrorReason(
    val status: Int,
    val code: String,
    val reason: String,
)
