package band.gosrock.api.config.security

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    private val webOriginPolicy: WebOriginPolicy
) : WebMvcConfigurer {

    // 프로필별 허용 출처 (#763). 운영에는 localhost·스테이징 출처가 없다
    override fun addCorsMappings(registry: CorsRegistry) {
        registry.addMapping("/**")
            .allowedMethods("*")
            .allowedOrigins(*webOriginPolicy.allowedOrigins().toTypedArray())
            .exposedHeaders("Set-Cookie")
            .allowCredentials(true)
    }
}
