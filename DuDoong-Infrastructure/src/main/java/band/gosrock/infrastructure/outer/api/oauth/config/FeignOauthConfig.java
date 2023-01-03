package band.gosrock.infrastructure.outer.api.oauth.config;


import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(KakaoOauthErrorDecoder.class)
public class FeignOauthConfig {

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public KakaoOauthErrorDecoder commonFeignErrorDecoder() {
        return new KakaoOauthErrorDecoder();
    }

    @Bean
    Encoder formEncoder() {
        return new feign.form.FormEncoder();
    }
}
