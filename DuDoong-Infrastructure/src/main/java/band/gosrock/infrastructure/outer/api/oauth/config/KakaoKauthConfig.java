package band.gosrock.infrastructure.outer.api.oauth.config;


import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import(KauthErrorDecoder.class)
public class KakaoKauthConfig {

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public KauthErrorDecoder commonFeignErrorDecoder() {
        return new KauthErrorDecoder();
    }

    @Bean
    Encoder formEncoder() {
        return new feign.form.FormEncoder();
    }
}
