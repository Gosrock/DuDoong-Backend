package band.gosrock.common.properties;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Getter
@AllArgsConstructor
@ConstructorBinding
@ConfigurationProperties("auth.jwt")
public class JwtProperties {
    private final String secretKey;
    private final Long accessExp;
    private final Long refreshExp;
}
