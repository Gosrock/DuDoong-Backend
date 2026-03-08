package band.gosrock.infrastructure.outer.api.tossPayments.config

import band.gosrock.common.properties.TossPaymentsProperties
import feign.auth.BasicAuthRequestInterceptor
import org.springframework.context.annotation.Bean

class TossHeaderConfig(private val tossPaymentsProperties: TossPaymentsProperties) {

    @Bean
    fun basicAuthRequestInterceptor(): BasicAuthRequestInterceptor =
        BasicAuthRequestInterceptor(tossPaymentsProperties.secretKey, "")
}
