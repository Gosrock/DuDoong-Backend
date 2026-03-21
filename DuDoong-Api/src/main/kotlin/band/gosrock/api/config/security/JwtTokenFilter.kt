package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.consts.DuDoongStatic
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.WebUtils

@Component
class JwtTokenFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userAdaptor: UserAdaptor,
    private val cookieHelper: CookieHelper,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = resolveToken(request)

        if (token != null) {
            val authentication = getAuthentication(token)
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }

    private fun resolveToken(request: HttpServletRequest): String? {
        // Admin 전용 헤더 우선
        val adminToken = request.getHeader(DuDoongStatic.ADMIN_TOKEN_HEADER)
        if (adminToken != null) {
            return adminToken
        }
        // 쿠키방식 지원
        val accessTokenCookie = WebUtils.getCookie(request, cookieHelper.getAccessTokenName())
        if (accessTokenCookie != null) {
            return accessTokenCookie.value
        }
        // 기존 jwt 방식 지원
        val rawHeader = request.getHeader(DuDoongStatic.AUTH_HEADER) ?: return null

        if (rawHeader.length > DuDoongStatic.BEARER.length &&
            rawHeader.startsWith(DuDoongStatic.BEARER)
        ) {
            return rawHeader.substring(DuDoongStatic.BEARER.length)
        }
        return null
    }

    fun getAuthentication(token: String): Authentication {
        val accessTokenInfo = jwtTokenProvider.parseAccessToken(token)
        val userId = accessTokenInfo.userId

        // 매 요청마다 DB에서 실시간 role 조회 → 역할 변경 즉시 반영
        val user = userAdaptor.queryUser(userId)
        val role = user.accountRole.value

        val userDetails = AuthDetails(userId.toString(), role)
        return UsernamePasswordAuthenticationToken(userDetails, "user", userDetails.authorities)
    }
}
