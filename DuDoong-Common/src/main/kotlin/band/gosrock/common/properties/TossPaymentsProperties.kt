package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("toss")
data class TossPaymentsProperties(
    val secretKey: String,
    val mid: String,
)
