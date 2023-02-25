package band.gosrock.api.config.security;


import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler;

@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final FilterConfig filterConfig;
    private static final String[] SwaggerPatterns = {
        "/swagger-resources/**", "/swagger-ui/**", "/v3/api-docs/**",
    };

    /** 스웨거용 인메모리 유저 설정 */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        UserDetails user =
                User.withUsername("user")
                        .password(passwordEncoder().encode("password"))
                        .roles("SWAGGER")
                        .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(8);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.formLogin().disable().cors().and().csrf().disable();

        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.authorizeRequests().expressionHandler(expressionHandler());

        // 베이직 시큐리티 설정
        // 할려면 filterConfig 에서 jwtToken filter 을 basic 보다 뒤로 밀어야함!
        http.authorizeRequests().mvcMatchers(SwaggerPatterns).authenticated().and().httpBasic();

        http.authorizeRequests()
                .mvcMatchers("/v1/auth/oauth/**")
                .permitAll()
                .mvcMatchers("/v1/auth/token/refresh")
                .permitAll()
                .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}")
                .permitAll()
                .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}/ticketItems")
                .permitAll()
                .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}/comments/**")
                .permitAll()
                .mvcMatchers(HttpMethod.GET, "/v1/events/search")
                .permitAll()
                .mvcMatchers(HttpMethod.POST, "/v1/coupons/campaigns")
                .hasRole("SUPER_ADMIN")
                .anyRequest()
                .authenticated();
        http.apply(filterConfig);

        return http.build();
    }

    @Bean
    public RoleHierarchyImpl roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        roleHierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_MANAGER > ROLE_USER");
        return roleHierarchy;
    }

    @Bean
    public DefaultWebSecurityExpressionHandler expressionHandler() {
        DefaultWebSecurityExpressionHandler expressionHandler =
                new DefaultWebSecurityExpressionHandler();
        expressionHandler.setRoleHierarchy(roleHierarchy());
        return expressionHandler;
    }
}
