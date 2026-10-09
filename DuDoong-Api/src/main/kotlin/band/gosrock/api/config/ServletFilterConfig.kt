package band.gosrock.api.config

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.ResourceUrlEncodingFilter

@Configuration
@Profile("prod", "staging", "dev")
class ServletFilterConfig(
    private val httpContentCacheFilter: HttpContentCacheFilter,
    private val mdcFilter: MdcFilter,
) : WebMvcConfigurer {

    @Bean
    fun setMdcFilterOrder(): FilterRegistrationBean<MdcFilter> {
        val registrationBean = FilterRegistrationBean<MdcFilter>()
        registrationBean.filter = mdcFilter
        registrationBean.order = Int.MIN_VALUE
        return registrationBean
    }

    @Bean
    fun setResourceUrlEncodingFilter(): FilterRegistrationBean<ResourceUrlEncodingFilter> {
        val registrationBean = FilterRegistrationBean<ResourceUrlEncodingFilter>()
        registrationBean.filter = ResourceUrlEncodingFilter()
        registrationBean.order = Int.MAX_VALUE - 2
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
