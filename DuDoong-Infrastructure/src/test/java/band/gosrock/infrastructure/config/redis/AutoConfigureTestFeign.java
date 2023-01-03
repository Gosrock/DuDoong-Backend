package band.gosrock.infrastructure.config.redis;


import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.cloud.openfeign.FeignAutoConfiguration;

@ImportAutoConfiguration(value = FeignAutoConfiguration.class)
@AutoConfigureJson
public @interface AutoConfigureTestFeign {}
