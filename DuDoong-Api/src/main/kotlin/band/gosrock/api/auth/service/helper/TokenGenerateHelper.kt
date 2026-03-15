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
    fun execute(user: User): TokenAndUserResponse {
        val userId = user.id!!
        val newAccessToken = jwtTokenProvider.generateAccessToken(
            userId, user.accountRole.value
        )
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(userId)

        val newRefreshTokenEntity = RefreshTokenEntity.builder()
            .refreshToken(newRefreshToken)
            .id(userId)
            .ttl(jwtTokenProvider.getRefreshTokenTTlSecond())
            .build()
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
