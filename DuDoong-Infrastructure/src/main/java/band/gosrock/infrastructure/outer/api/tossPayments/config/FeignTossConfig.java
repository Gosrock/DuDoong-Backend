package band.gosrock.infrastructure.outer.api.tossPayments.config;


import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

@Import({TossErrorDecoder.class, TossHeaderConfig.class})
public class FeignTossConfig {

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public TossErrorDecoder commonFeignErrorDecoder() {
        return new TossErrorDecoder();
    }

    @Bean
    public FeignFormatterRegistrar localDateFeignFormatterRegistrar() {
        return formatterRegistry -> {
            DateTimeFormatterRegistrar registrar = new DateTimeFormatterRegistrar();
            registrar.setUseIsoFormat(true);
            registrar.registerFormatters(formatterRegistry);
        };
    }
}
