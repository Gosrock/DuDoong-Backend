package band.gosrock.api.config.security;


import java.util.ArrayList;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig implements WebMvcConfigurer {
    private final Environment env;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] activeProfiles = env.getActiveProfiles();
        ArrayList<String> allowedOriginPatterns = new ArrayList<>();
        allowedOriginPatterns.add("https://dudoong.com");
        allowedOriginPatterns.add("https://staging.dudoong.com");
        if (!Arrays.stream(activeProfiles).toList().contains("prod")) {
            allowedOriginPatterns.add("http://localhost:3000");
        }
        String[] patterns = allowedOriginPatterns.toArray(String[]::new);
        registry.addMapping("/**")
                .allowedMethods("*")
                .allowedOriginPatterns(patterns)
                .exposedHeaders("Set-Cookie")
                .allowCredentials(true);
    }
}
