package band.gosrock.infrastructure.outer.api.tossPayments.config

import feign.codec.ErrorDecoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.cloud.openfeign.FeignFormatterRegistrar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar

@Import(TossHeaderConfig::class, PaymentCancelErrorDecoder::class)
class PaymentsCancelConfig {

    @Bean
    @ConditionalOnMissingBean(value = [ErrorDecoder::class])
    fun commonFeignErrorDecoder(): PaymentCancelErrorDecoder = PaymentCancelErrorDecoder()

    @Bean
    fun localDateFeignFormatterRegistrar(): FeignFormatterRegistrar =
        FeignFormatterRegistrar { formatterRegistry ->
            DateTimeFormatterRegistrar().apply {
                setUseIsoFormat(true)
                registerFormatters(formatterRegistry)
            }
        }
}
