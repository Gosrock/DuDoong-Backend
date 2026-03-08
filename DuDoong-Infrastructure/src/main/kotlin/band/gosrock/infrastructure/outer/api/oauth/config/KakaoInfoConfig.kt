package band.gosrock.infrastructure.outer.api.oauth.config

import feign.codec.Encoder
import feign.codec.ErrorDecoder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@Import(KakaoInfoErrorDecoder::class)
class KakaoInfoConfig {

    @Bean
    @ConditionalOnMissingBean(value = [ErrorDecoder::class])
    fun commonFeignErrorDecoder(): KakaoInfoErrorDecoder = KakaoInfoErrorDecoder()

    @Bean
    fun formEncoder(): Encoder = feign.form.FormEncoder()
}
