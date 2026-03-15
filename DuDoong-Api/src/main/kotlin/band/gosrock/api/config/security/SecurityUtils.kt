package band.gosrock.api.config.security

import band.gosrock.common.exception.SecurityContextNotFoundException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.CollectionUtils

object SecurityUtils {

    private val anonymous = SimpleGrantedAuthority("ROLE_ANONYMOUS")
    private val swagger = SimpleGrantedAuthority("ROLE_SWAGGER")
    private val notUserAuthority = listOf(anonymous, swagger)

    @JvmStatic
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw SecurityContextNotFoundException.EXCEPTION

        if (authentication.isAuthenticated &&
            !CollectionUtils.containsAny(authentication.authorities, notUserAuthority)
        ) {
            return authentication.name.toLong()
        }
        // 스웨거 유저일시 익명 유저 취급
        // 익명유저시 userId 0 반환
        return 0L
    }
}
