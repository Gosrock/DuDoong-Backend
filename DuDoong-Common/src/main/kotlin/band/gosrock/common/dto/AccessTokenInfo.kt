package band.gosrock.common.dto

data class AccessTokenInfo(
    val userId: Long,
    val isAdmin: Boolean = false,
)
