package band.gosrock.api.config.rateLimit

import org.springframework.stereotype.Component
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Component
class ThrottlingWebConfigure(
    private val throttlingInterceptor: ThrottlingInterceptor,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(throttlingInterceptor)
    }
}
