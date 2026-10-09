package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.consts.DuDoongStatic
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
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
            getAuthentication(token)?.let { SecurityContextHolder.getContext().authentication = it }
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

    /** 정상이 아닌 계정이면 null (익명 처리) */
    fun getAuthentication(token: String): Authentication? {
        val accessTokenInfo = jwtTokenProvider.parseAccessToken(token)
        val userId = accessTokenInfo.userId

        // 매 요청마다 DB에서 실시간 role 조회 → 역할 변경 즉시 반영
        val user = userAdaptor.queryUser(userId)
        // 정지·탈퇴 계정의 토큰은 발급 이후에도 쓸 수 없다. 예외 대신 익명으로 둔다:
        // 보호 경로는 entry point 가 401, 공개 경로·로그인·로그아웃(쿠키 삭제)은 그대로 동작한다 (재로그인은 403 USER_403_1)
        if (user.accountState != AccountState.NORMAL) return null
        val role = user.accountRole.value

        val userDetails = AuthDetails(userId.toString(), role)
        return UsernamePasswordAuthenticationToken(userDetails, "user", userDetails.authorities)
    }
}
