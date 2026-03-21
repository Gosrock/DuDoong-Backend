package band.gosrock.api.config.security

import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenFilter: JwtTokenFilter,
    private val accessDeniedFilter: AccessDeniedFilter,
    private val jwtExceptionFilter: JwtExceptionFilter,
    private val springEnvironmentHelper: SpringEnvironmentHelper
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .formLogin { it.disable() }
            .cors {}
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/api/v1/auth/oauth/**").permitAll()
                .requestMatchers("/api/v1/auth/token/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/ticketItems").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/examples/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/coupons/campaigns").hasRole("SUPER_ADMIN")
                .requestMatchers("/internal-api/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                .anyRequest().hasRole("USER")
        }

        http.addFilterBefore(jwtTokenFilter, BasicAuthenticationFilter::class.java)
        http.addFilterBefore(jwtExceptionFilter, JwtTokenFilter::class.java)
        http.addFilterBefore(accessDeniedFilter, JwtTokenFilter::class.java)

        return http.build()
    }

    @Bean
    fun roleHierarchy(): RoleHierarchyImpl {
        val roleHierarchy = RoleHierarchyImpl()
        roleHierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_USER")
        return roleHierarchy
    }
}
