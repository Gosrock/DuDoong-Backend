package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cookie")
data class CookieProperties(
    val domain: String,
)
