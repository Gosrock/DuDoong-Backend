package band.gosrock.api.auth.service

import band.gosrock.api.config.security.SecurityUtils
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor

@UseCase
class LogoutUseCase(
    private val refreshTokenAdaptor: RefreshTokenAdaptor
) {

    fun execute() {
        val currentUserId = SecurityUtils.getCurrentUserId()
        refreshTokenAdaptor.deleteByUserId(currentUserId)
    }
}
