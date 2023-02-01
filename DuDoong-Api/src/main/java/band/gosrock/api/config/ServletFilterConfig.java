package band.gosrock.api.config;

import javax.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.context.AbstractSecurityWebApplicationInitializer;
import org.springframework.web.filter.ForwardedHeaderFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class ServletFilterConfig implements WebMvcConfigurer {

    private final HttpContentCacheFilter httpContentCacheFilter;
    private final ForwardedHeaderFilter forwardedHeaderFilter;

    @Bean
    public FilterRegistrationBean securityFilterChain(@Qualifier(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME) Filter securityFilter) {
        FilterRegistrationBean registration = new FilterRegistrationBean(securityFilter);
        registration.setOrder(Integer.MAX_VALUE - 2);
        registration.setName(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
        return registration;
    }

    @Bean
    public FilterRegistrationBean afsd() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//        ForwardedHeaderFilter forwardedHeaderFilter = new ForwardedHeaderFilter();
        registrationBean.setFilter(forwardedHeaderFilter);
        registrationBean.setOrder(Integer.MAX_VALUE - 1);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean userInsertingMdcFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
//        HttpContentCacheFilter httpContentCacheFilter = new HttpContentCacheFilter();
        registrationBean.setFilter(httpContentCacheFilter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

}
