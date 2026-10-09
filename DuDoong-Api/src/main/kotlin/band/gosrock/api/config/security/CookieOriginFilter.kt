package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.consts.DuDoongStatic
import band.gosrock.common.exception.OriginNotAllowedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 쿠키로 인증된 상태 변경 요청의 출처 확인 (#763).
 *
 * POST·PUT·PATCH·DELETE 이고 accessToken 쿠키가 있으며 Authorization·X-Admin-Token 헤더가 없으면,
 * Origin(없으면 Referer 의 출처)이 [WebOriginPolicy] 허용 출처여야 한다. 둘 다 없으면 거부한다.
 * 헤더로 토큰을 보내는 요청(앱·서버 간 호출·E2E)과 쿠키 없는 요청은 그대로 통과한다.
 *
 * 제외: 토큰 재발급(프론트 SSR 이 브라우저 쿠키를 실어 Origin 없이 부른다), 로그아웃(쿠키 삭제는 항상 되어야 한다)
 */
@Component
class CookieOriginFilter(
    private val cookieHelper: CookieHelper,
    private val webOriginPolicy: WebOriginPolicy,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method !in STATE_CHANGING_METHODS || EXEMPT_PATH_SUFFIXES.any { request.requestURI.endsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (!hasHeaderToken(request) && cookieHelper.hasAccessTokenCookie(request)) {
            val origin = request.getHeader(ORIGIN) ?: originOf(request.getHeader(REFERER))
            if (!webOriginPolicy.isAllowed(origin)) {
                log.warn("[AUTH] 쿠키 인증 요청 출처 거부 - method={}, uri={}, origin={}", request.method, request.requestURI, origin ?: "없음")
                throw OriginNotAllowedException.EXCEPTION
            }
        }
        filterChain.doFilter(request, response)
    }

    private fun hasHeaderToken(request: HttpServletRequest): Boolean =
        !request.getHeader(DuDoongStatic.AUTH_HEADER).isNullOrBlank() ||
            !request.getHeader(DuDoongStatic.ADMIN_TOKEN_HEADER).isNullOrBlank()

    private fun originOf(referer: String?): String? {
        if (referer.isNullOrBlank()) return null
        return try {
            val uri = URI(referer)
            if (uri.scheme == null || uri.host == null) return null
            val port = if (uri.port == -1) "" else ":${uri.port}"
            "${uri.scheme}://${uri.host}$port"
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val ORIGIN = "Origin"
        private const val REFERER = "Referer"
        private val STATE_CHANGING_METHODS = setOf("POST", "PUT", "PATCH", "DELETE")
        private val EXEMPT_PATH_SUFFIXES = listOf("/v1/auth/token/refresh", "/v1/auth/logout")
    }
}
