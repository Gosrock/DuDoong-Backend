package band.gosrock.common.config;

import band.gosrock.common.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties({JwtProperties.class})
@Configuration
public class ConfigurationPropertiesConfig {}
