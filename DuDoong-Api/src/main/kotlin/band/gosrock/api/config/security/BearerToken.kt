package band.gosrock.api.config.security

import band.gosrock.common.consts.DuDoongStatic
import jakarta.servlet.http.HttpServletRequest

/**
 * `Authorization: Bearer {토큰}` 헤더의 토큰 (#763). 인증([JwtTokenFilter])과 출처 검사 면제([CookieOriginFilter])가
 * 같은 기준을 쓰도록 한 곳에 둔다. `Bearer ` 로 시작하고 뒤에 값이 있을 때만 토큰으로 본다.
 */
object BearerToken {
    fun from(request: HttpServletRequest): String? {
        val rawHeader = request.getHeader(DuDoongStatic.AUTH_HEADER) ?: return null
        if (rawHeader.length > DuDoongStatic.BEARER.length && rawHeader.startsWith(DuDoongStatic.BEARER)) {
            return rawHeader.substring(DuDoongStatic.BEARER.length)
        }
        return null
    }
}
