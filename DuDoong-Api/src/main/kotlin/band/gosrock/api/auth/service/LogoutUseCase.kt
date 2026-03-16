package band.gosrock.api.auth.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor

@UseCase
class LogoutUseCase(
    private val refreshTokenAdaptor: RefreshTokenAdaptor
) {

    fun execute(userId: Long) {
        refreshTokenAdaptor.deleteByUserId(userId)
    }
}
