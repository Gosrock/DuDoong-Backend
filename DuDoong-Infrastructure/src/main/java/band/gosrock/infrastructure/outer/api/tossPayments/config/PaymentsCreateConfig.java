package band.gosrock.infrastructure.outer.api.tossPayments.config;


import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

@Import({TossHeaderConfig.class, PaymentCreateErrorDecoder.class})
public class PaymentsCreateConfig {

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public PaymentCreateErrorDecoder commonFeignErrorDecoder() {
        return new PaymentCreateErrorDecoder();
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
