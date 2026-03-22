package band.gosrock.api.auth.service.helper

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.common.annotation.Helper
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.common.dto.ProfileViewDto
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.domain.RefreshTokenEntity
import band.gosrock.domain.domains.user.domain.User
import org.springframework.transaction.annotation.Transactional

@Helper
class TokenGenerateHelper(
    private val jwtTokenProvider: JwtTokenProvider,
    private val refreshTokenAdaptor: RefreshTokenAdaptor
) {

    @Transactional
    fun execute(user: User): TokenAndUserResponse =
        generateTokenResponse(user, admin = false)

    @Transactional
    fun executeAdmin(user: User): TokenAndUserResponse =
        generateTokenResponse(user, admin = true)

    private fun generateTokenResponse(user: User, admin: Boolean): TokenAndUserResponse {
        val userId = user.id!!
        val newAccessToken = if (admin) {
            jwtTokenProvider.generateAdminAccessToken(userId)
        } else {
            jwtTokenProvider.generateAccessToken(userId)
        }
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(userId)

        val newRefreshTokenEntity = RefreshTokenEntity(
            refreshToken = newRefreshToken,
            id = userId,
            ttl = jwtTokenProvider.getRefreshTokenTTlSecond(),
        )
        refreshTokenAdaptor.save(newRefreshTokenEntity)

        return TokenAndUserResponse(
            userProfile = ProfileViewDto.from(user),
            accessToken = newAccessToken,
            accessTokenAge = jwtTokenProvider.getAccessTokenTTlSecond(),
            refreshTokenAge = jwtTokenProvider.getRefreshTokenTTlSecond(),
            refreshToken = newRefreshToken
        )
    }
}
