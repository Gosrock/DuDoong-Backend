package band.gosrock.api.config.security

import band.gosrock.common.consts.DuDoongStatic
import band.gosrock.common.jwt.JwtTokenProvider
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
    private val jwtTokenProvider: JwtTokenProvider
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
        // 쿠키방식 지원
        val accessTokenCookie = WebUtils.getCookie(request, "accessToken")
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
        val userDetails = AuthDetails(accessTokenInfo.userId.toString(), accessTokenInfo.role)
        return UsernamePasswordAuthenticationToken(userDetails, "user", userDetails.authorities)
    }
}
