package band.gosrock.api.config.security

import band.gosrock.common.consts.DuDoongStatic.SwaggerPatterns
import band.gosrock.common.helper.SpringEnvironmentHelper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
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
import org.springframework.security.web.access.expression.DefaultWebSecurityExpressionHandler

@EnableWebSecurity
class SecurityConfig(
    private val filterConfig: FilterConfig,
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
        http.formLogin().disable().cors().and().csrf().disable()
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        http.authorizeRequests().expressionHandler(expressionHandler())

        // 베이직 시큐리티 설정
        // 개발 환경에서만 스웨거 비밀번호 미설정.
        if (springEnvironmentHelper.isProdAndStagingProfile()) {
            http.authorizeRequests().mvcMatchers(*SwaggerPatterns).authenticated().and().httpBasic()
        }

        http.authorizeRequests()
            .mvcMatchers(*SwaggerPatterns).permitAll()
            .mvcMatchers("/v1/auth/oauth/**").permitAll()
            .mvcMatchers("/v1/auth/token/refresh").permitAll()
            .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}").permitAll()
            .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}/ticketItems").permitAll()
            .mvcMatchers(HttpMethod.GET, "/v1/events/{eventId:[0-9]*$}/comments/**").permitAll()
            .mvcMatchers(HttpMethod.GET, "/v1/events/search").permitAll()
            .mvcMatchers(HttpMethod.GET, "/v1/examples/health").permitAll()
            .mvcMatchers(HttpMethod.POST, "/v1/coupons/campaigns").hasRole("SUPER_ADMIN")
            .anyRequest().hasRole("USER")
        http.apply(filterConfig)

        return http.build()
    }

    @Bean
    fun roleHierarchy(): RoleHierarchyImpl {
        val roleHierarchy = RoleHierarchyImpl()
        roleHierarchy.setHierarchy("ROLE_SUPER_ADMIN > ROLE_ADMIN > ROLE_MANAGER > ROLE_USER")
        return roleHierarchy
    }

    @Bean
    fun expressionHandler(): DefaultWebSecurityExpressionHandler {
        val expressionHandler = DefaultWebSecurityExpressionHandler()
        expressionHandler.setRoleHierarchy(roleHierarchy())
        return expressionHandler
    }
}
