package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("toss")
data class TossPaymentsProperties(
    val secretKey: String,
    val mid: String,
)
