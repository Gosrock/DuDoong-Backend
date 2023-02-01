package band.gosrock.api.config.security;


import lombok.RequiredArgsConstructor;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class FilterConfig
        extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {

    private final JwtTokenFilter jwtTokenFilter;
    private final AccessDeniedFilter accessDeniedFilter;

    private final JwtExceptionFilter jwtExceptionFilter;

    @Override
    public void configure(HttpSecurity builder) {
        builder.addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class);
        builder.addFilterBefore(jwtExceptionFilter, JwtTokenFilter.class);
        builder.addFilterAfter(accessDeniedFilter, ExceptionTranslationFilter.class);
    }
}
