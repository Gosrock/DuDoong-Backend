package band.gosrock.infrastructure.outer.api.tossPayments.config;


import band.gosrock.infrastructure.outer.api.BaseFeignClientPackage;
import band.gosrock.infrastructure.outer.api.oauth.config.FeignClientErrorDecoder;
import feign.Logger.Level;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignFormatterRegistrar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.format.datetime.standard.DateTimeFormatterRegistrar;

@EnableFeignClients(basePackageClasses = BaseFeignClientPackage.class)
@ComponentScan(basePackageClasses = BaseFeignClientPackage.class)
@Import({FeignClientErrorDecoder.class, TossHeaderConfig.class})
public class FeignTossConfig {

    @Bean
    Level feignLoggerLevel() {
        return Level.FULL;
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public FeignClientErrorDecoder commonFeignErrorDecoder() {
        return new FeignClientErrorDecoder();
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
