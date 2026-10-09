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

    /**
     * 순서: Authorization Bearer 헤더 → (레거시) X-Admin-Token → accessToken 쿠키.
     * 헤더를 쿠키보다 먼저 본다 (#763): Origin 검사를 면제받는 토큰([CookieOriginFilter])과 실제 인증 토큰이 같아야 한다.
     * 프론트는 헤더와 쿠키에 같은 사용자의 토큰을 함께 보낸다(헤더 쪽이 같거나 더 최근 값)
     */
    private fun resolveToken(request: HttpServletRequest): String? {
        BearerToken.from(request)?.let { return it }
        request.getHeader(DuDoongStatic.ADMIN_TOKEN_HEADER)?.let { return it }
        return cookieHelper.getAccessTokenFromRequest(request)
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
