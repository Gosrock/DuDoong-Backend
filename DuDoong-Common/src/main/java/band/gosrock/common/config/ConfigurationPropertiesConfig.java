package band.gosrock.common.config;


import band.gosrock.common.properties.JwtProperties;
import band.gosrock.common.properties.OauthProperties;
import band.gosrock.common.properties.TossPaymentsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({
    JwtProperties.class,
    OauthProperties.class,
    TossPaymentsProperties.class
})
@Configuration
public class ConfigurationPropertiesConfig {}
