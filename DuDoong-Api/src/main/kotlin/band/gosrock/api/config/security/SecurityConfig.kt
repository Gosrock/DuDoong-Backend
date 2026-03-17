package band.gosrock.api.config.security

import band.gosrock.common.consts.DuDoongStatic.SwaggerPatterns
import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.User
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.provisioning.InMemoryUserDetailsManager
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtTokenFilter: JwtTokenFilter,
    private val accessDeniedFilter: AccessDeniedFilter,
    private val jwtExceptionFilter: JwtExceptionFilter,
    @Value("\${swagger.user}") private val swaggerUser: String,
    @Value("\${swagger.password}") private val swaggerPassword: String,
    private val springEnvironmentHelper: SpringEnvironmentHelper
) {

    @Bean
    fun userDetailsService(): InMemoryUserDetailsManager {
        val user = User.withUsername(swaggerUser)
            .password(passwordEncoder().encode(swaggerPassword))
            .roles("SWAGGER")
            .build()
        return InMemoryUserDetailsManager(user)
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(8)

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .formLogin { it.disable() }
            .cors {}
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        if (springEnvironmentHelper.isProdAndStagingProfile()) {
            http.authorizeHttpRequests { auth ->
                auth.requestMatchers(*SwaggerPatterns).authenticated()
            }.httpBasic {}
        }

        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers(*SwaggerPatterns).permitAll()
                .requestMatchers("/api/v1/auth/oauth/**").permitAll()
                .requestMatchers("/api/v1/auth/token/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/ticketItems").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/examples/health").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/coupons/campaigns").hasRole("SUPER_ADMIN")
                .requestMatchers("/internal-api/v1/auth/oauth/**").permitAll()
                .requestMatchers("/internal-api/v1/auth/token/refresh").permitAll()
                .requestMatchers("/internal-api/**").access { authentication, _ ->
                    // MANAGER 이상 역할 + admin 토큰(aud:admin) 필수
                    val authn = authentication.get()
                        ?: return@access org.springframework.security.authorization.AuthorizationDecision(false)
                    val principal = authn.principal
                    if (principal !is AuthDetails) {
                        return@access org.springframework.security.authorization.AuthorizationDecision(false)
                    }
                    val hasRole = authn.authorities.any {
                        it.authority == "ROLE_MANAGER" || it.authority == "ROLE_ADMIN" || it.authority == "ROLE_SUPER_ADMIN"
                    }
                    org.springframework.security.authorization.AuthorizationDecision(hasRole && principal.isAdmin)
                }
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
        roleHierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_MANAGER > ROLE_USER")
        return roleHierarchy
    }
}
