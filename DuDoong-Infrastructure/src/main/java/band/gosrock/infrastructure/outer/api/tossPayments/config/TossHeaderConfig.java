package band.gosrock.infrastructure.outer.api.tossPayments.config;


import band.gosrock.common.properties.TossPaymentsProperties;
import feign.auth.BasicAuthRequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
public class TossHeaderConfig {

    private final TossPaymentsProperties tossPaymentsProperties;

    @Bean
    public BasicAuthRequestInterceptor basicAuthRequestInterceptor() {
        return new BasicAuthRequestInterceptor(tossPaymentsProperties.getSecretKey(), "");
    }
}
