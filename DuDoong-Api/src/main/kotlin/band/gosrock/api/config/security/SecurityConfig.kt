package band.gosrock.api.config.security

import band.gosrock.common.dto.ErrorResponse
import band.gosrock.common.exception.GlobalErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
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
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .formLogin { it.disable() }
            .cors {}
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

        http.exceptionHandling { exceptions ->
            exceptions.accessDeniedHandler { request, response, _ ->
                val errorResponse = ErrorResponse(
                    GlobalErrorCode.ACCESS_TOKEN_NOT_EXIST.getErrorReason(),
                    request.requestURL.toString()
                )
                response.characterEncoding = "UTF-8"
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.status = 403
                response.writer.write(objectMapper.writeValueAsString(errorResponse))
            }
            exceptions.authenticationEntryPoint { request, response, _ ->
                val errorResponse = ErrorResponse(
                    GlobalErrorCode.ACCESS_TOKEN_NOT_EXIST.getErrorReason(),
                    request.requestURL.toString()
                )
                response.characterEncoding = "UTF-8"
                response.contentType = MediaType.APPLICATION_JSON_VALUE
                response.status = 401
                response.writer.write(objectMapper.writeValueAsString(errorResponse))
            }
        }

        http.authorizeHttpRequests { auth ->
            auth
                .requestMatchers("/api/v1/auth/oauth/**").permitAll()
                .requestMatchers("/api/v1/auth/token/refresh").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/ticketItems").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/{eventId:[0-9]*$}/comments/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/examples/health").permitAll()
                .requestMatchers(HttpMethod.GET, *V2_PUBLIC_GET_PATHS.toTypedArray()).permitAll()
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

    companion object {
        /** 인증 없이 접근 가능한 v2 GET 경로. v2 공개 API 추가 시 여기에만 등록한다. */
        private val V2_PUBLIC_GET_PATHS: List<String> = listOf(
            "/api/v2/health",
            // 공개 호스트 홈 / 호스트 공연 리스트 (비로그인 시 userId = 0)
            "/api/v2/hosts/{hostId:[0-9]+}",
            "/api/v2/hosts/{hostId:[0-9]+}/events",
            // 공연 태그 목록 / 공연 섹션 (준비중 공연 섹션은 유스케이스에서 멤버만 허용)
            "/api/v2/tags",
            "/api/v2/events/{eventId:[0-9]+}/sections",
            // 사용자 앱 공연 탐색 (#716): 홈 / 공연 리스트 / 공개 상세 / 판매 중 티켓 (준비중·삭제 공연은 유스케이스에서 404)
            "/api/v2/home",
            "/api/v2/events",
            "/api/v2/events/{eventId:[0-9]+}",
            "/api/v2/events/{eventId:[0-9]+}/ticket-items",
        )
    }
}
