package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.exception.OriginNotAllowedException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.net.URI
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * 쿠키로 인증된 상태 변경 요청의 출처 확인 (#763).
 *
 * POST·PUT·PATCH·DELETE 이고 accessToken 쿠키가 있으며 `Authorization: Bearer` 헤더가 없으면,
 * Origin(없으면 Referer 의 출처)이 [WebOriginPolicy] 허용 출처여야 한다. 둘 다 없으면 거부 대상이다.
 * Bearer 헤더가 있으면 [JwtTokenFilter] 가 쿠키보다 헤더 토큰으로 인증하므로 검사하지 않는다. 쿠키 없는 요청도 통과한다.
 *
 * `auth.origin-check.enforce` (기본 false = report-only): 꺼져 있으면 막지 않고 `[AUTH] origin-check would-reject` 경고만 남긴다.
 * 운영 로그로 오탐이 없는지 확인한 뒤 기본값을 true 로 바꾼다. 켜져 있으면 403 AUTH_403_3.
 *
 * 제외: 토큰 재발급(프론트 SSR 이 브라우저 쿠키를 실어 Origin 없이 부른다), 로그아웃(쿠키 삭제는 항상 되어야 한다)
 */
@Component
class CookieOriginFilter(
    private val cookieHelper: CookieHelper,
    private val webOriginPolicy: WebOriginPolicy,
    @Value("\${auth.origin-check.enforce:false}") private val enforce: Boolean = false,
) : OncePerRequestFilter() {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean =
        request.method !in STATE_CHANGING_METHODS || EXEMPT_PATH_SUFFIXES.any { request.requestURI.endsWith(it) }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        if (BearerToken.from(request) == null && cookieHelper.hasAccessTokenCookie(request)) {
            val origin = request.getHeader(ORIGIN)
            val refererOrigin = originOf(request.getHeader(REFERER))
            if (!webOriginPolicy.isAllowed(origin ?: refererOrigin)) {
                // 토큰·쿠키 값은 남기지 않는다
                if (!enforce) {
                    log.warn(
                        "[AUTH] origin-check would-reject - method={}, uri={}, origin={}, refererOrigin={}",
                        request.method, request.requestURI, origin ?: "없음", refererOrigin ?: "없음",
                    )
                } else {
                    log.warn(
                        "[AUTH] origin-check reject - method={}, uri={}, origin={}, refererOrigin={}",
                        request.method, request.requestURI, origin ?: "없음", refererOrigin ?: "없음",
                    )
                    throw OriginNotAllowedException.EXCEPTION
                }
            }
        }
        filterChain.doFilter(request, response)
    }

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
