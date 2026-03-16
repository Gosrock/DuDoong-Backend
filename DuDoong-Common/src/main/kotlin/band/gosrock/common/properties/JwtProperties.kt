package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(
    val secretKey: String,
    val accessExp: Long,
    val refreshExp: Long,
)
