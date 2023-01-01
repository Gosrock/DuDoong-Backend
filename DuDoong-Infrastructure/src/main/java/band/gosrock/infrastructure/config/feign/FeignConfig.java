package band.gosrock.infrastructure.config.feign;


import band.gosrock.infrastructure.DuDoongInfraApplication;
import feign.Logger.Level;
import feign.codec.ErrorDecoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@EnableFeignClients(basePackageClasses = DuDoongInfraApplication.class)
@Import(FeignClientErrorDecoder.class)
@Configuration
public class FeignConfig {

    @Bean
    Level feignLoggerLevel() {
        return Level.FULL;
    }

    @Bean
    @ConditionalOnMissingBean(value = ErrorDecoder.class)
    public FeignClientErrorDecoder commonFeignErrorDecoder() {
        return new FeignClientErrorDecoder();
    }
}
