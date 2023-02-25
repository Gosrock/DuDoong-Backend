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

    //    private final SpringEnvironmentHelper springEnvironmentHelper;

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
        // 베이직 시큐리티는 ExceptionTranslationFilter 에서 authenticationEntryPoint 에서
        // commence 로 401 넘겨줌. -> 응답 헤더에 www-authenticate 로 인증하라는 응답줌.
        // 브라우저가 basic auth 실행 시켜줌.
        //        if(springEnvironmentHelper.isProdAndStagingProfile()){
        http.authorizeRequests().mvcMatchers(SwaggerPatterns).authenticated().and().httpBasic();
        //        }

        http.authorizeRequests()
                .mvcMatchers(SwaggerPatterns)
                .permitAll()
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
                // 인증 이필요한 모든 요청은 USER 권한을 최소한 가지고있어야한다.
                // 스웨거용 인메모리 유저의 권한은 SWAGGER 이다
                // 따라서 스웨거용 인메모리 유저가 basic auth 필터를 통과해서 들어오더라도
                // ( jwt 필터나 , basic auth 필터의 순서는 상관이없다.) --> 왜냐면 jwt는 토큰 여부 파악만하고 있으면 검증이고 없으면 넘김.
                // 내부 소스까지 실행을 못함. 권한 문제 때문에.
                .anyRequest()
                .hasRole("USER");
        //        http
        //            .exceptionHandling()
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
