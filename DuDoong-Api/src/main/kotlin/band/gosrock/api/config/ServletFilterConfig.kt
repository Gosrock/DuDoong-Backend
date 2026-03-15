package band.gosrock.api.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer
import org.springframework.web.filter.ForwardedHeaderFilter
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter
import javax.servlet.Filter

@Configuration
@Profile("prod", "staging", "dev")
class ServletFilterConfig(
    private val httpContentCacheFilter: HttpContentCacheFilter,
    private val forwardedHeaderFilter: ForwardedHeaderFilter,
) : WebMvcConfigurer {

    @Bean
    fun securityFilterChain(
        @Qualifier(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
        securityFilter: Filter,
    ): FilterRegistrationBean<Filter> {
        val registration = FilterRegistrationBean(securityFilter)
        registration.order = Int.MAX_VALUE - 3
        registration.setName(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME)
        return registration
    }

    @Bean
    fun setResourceUrlEncodingFilter(): FilterRegistrationBean<ResourceUrlEncodingFilter> {
        val registrationBean = FilterRegistrationBean<ResourceUrlEncodingFilter>()
        registrationBean.filter = ResourceUrlEncodingFilter()
        registrationBean.order = Int.MAX_VALUE - 2
        return registrationBean
    }

    @Bean
    fun setForwardedHeaderFilterOrder(): FilterRegistrationBean<ForwardedHeaderFilter> {
        val registrationBean = FilterRegistrationBean<ForwardedHeaderFilter>()
        registrationBean.filter = forwardedHeaderFilter
        registrationBean.order = Int.MAX_VALUE - 1
        return registrationBean
    }

    @Bean
    fun setHttpContentCacheFilterOrder(): FilterRegistrationBean<HttpContentCacheFilter> {
        val registrationBean = FilterRegistrationBean<HttpContentCacheFilter>()
        registrationBean.filter = httpContentCacheFilter
        registrationBean.order = Int.MAX_VALUE
        return registrationBean
    }
}
