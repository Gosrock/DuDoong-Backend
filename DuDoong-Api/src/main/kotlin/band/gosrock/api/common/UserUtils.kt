package band.gosrock.api.common

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.User
import org.springframework.stereotype.Component

@Component
class UserUtils(
    private val userAdaptor: UserAdaptor,
) {
    fun getCurrentUserId(): Long = SecurityUtils.getCurrentUserId()

    fun getCurrentUser(): User = userAdaptor.queryUser(getCurrentUserId())
}
