package band.gosrock.admin.service

import band.gosrock.admin.exception.AdminForbiddenException
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AdminAuthValidator(
    private val userAdaptor: UserAdaptor,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun validateAdminOrAbove(userId: Long): User {
        val user = userAdaptor.queryUser(userId)
        if (user.accountRole != AccountRole.ADMIN && user.accountRole != AccountRole.SUPER_ADMIN) {
            log.info("[ADMIN-AUTH] DENIED - userId={}, role={}, required=ADMIN+", userId, user.accountRole)
            throw AdminForbiddenException.EXCEPTION
        }
        log.info("[ADMIN-AUTH] GRANTED - userId={}, role={}", userId, user.accountRole)
        return user
    }

    fun validateSuperAdmin(userId: Long): User {
        val user = userAdaptor.queryUser(userId)
        if (user.accountRole != AccountRole.SUPER_ADMIN) {
            log.info("[ADMIN-AUTH] DENIED - userId={}, role={}, required=SUPER_ADMIN", userId, user.accountRole)
            throw AdminForbiddenException.EXCEPTION
        }
        log.info("[ADMIN-AUTH] GRANTED - userId={}, role=SUPER_ADMIN", userId)
        return user
    }
}
