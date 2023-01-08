package band.gosrock.infrastructure.config.feign;


import band.gosrock.infrastructure.outer.api.BaseFeignClientPackage;
import feign.Logger.Level;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackageClasses = BaseFeignClientPackage.class)
public class FeginCommonConfig {
    @Bean
    Level feignLoggerLevel() {
        return Level.FULL;
    }
}
