package band.gosrock.domain.domains.user.adaptor

import band.gosrock.common.annotation.Adaptor
import band.gosrock.common.exception.RefreshTokenExpiredException
import band.gosrock.domain.domains.user.domain.RefreshTokenEntity
import band.gosrock.domain.domains.user.repository.RefreshTokenRepository

@Adaptor
class RefreshTokenAdaptor(private val refreshTokenRepository: RefreshTokenRepository) {

    fun queryRefreshToken(refreshToken: String): RefreshTokenEntity =
        refreshTokenRepository.findByRefreshToken(refreshToken)
            .orElseThrow { RefreshTokenExpiredException.EXCEPTION }

    fun save(refreshToken: RefreshTokenEntity): RefreshTokenEntity =
        refreshTokenRepository.save(refreshToken)

    fun deleteByUserId(userId: Long) {
        refreshTokenRepository.deleteById(userId.toString())
    }
}
