package band.gosrock.api.config.security

import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class CorsConfig(
    private val springEnvironmentHelper: SpringEnvironmentHelper
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val allowedOriginPatterns = mutableListOf(
            "https://dudoong.com",
            "https://staging.dudoong.com",
            "https://internal-admin.dudoong.com",
            "https://staging-internal-admin.dudoong.com",
            "http://localhost:3000",
            "http://localhost:5173"
        )
        registry.addMapping("/**")
            .allowedMethods("*")
            .allowedOriginPatterns(*allowedOriginPatterns.toTypedArray())
            .exposedHeaders("Set-Cookie")
            .allowCredentials(true)
    }
}
