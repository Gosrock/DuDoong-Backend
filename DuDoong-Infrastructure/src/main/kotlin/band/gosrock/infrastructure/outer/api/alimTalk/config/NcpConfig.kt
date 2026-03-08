package band.gosrock.infrastructure.outer.api.alimTalk.config

import feign.RequestInterceptor
import feign.RequestTemplate
import feign.codec.ErrorDecoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(NcpErrorDecoder::class)
class NcpConfig {
    @Bean
    @ConditionalOnMissingBean(value = [ErrorDecoder::class])
    fun commonFeignErrorDecoder(): NcpErrorDecoder = NcpErrorDecoder()

    @Bean
    fun basicAuthRequestInterceptor(): RequestInterceptor = ColonInterceptor()

    class ColonInterceptor : RequestInterceptor {
        override fun apply(template: RequestTemplate) {
            template.uri(template.path().replace("%3A", ":"))
        }
    }
}
