package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.domains.user.adaptor.RefreshTokenAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.service.UserDomainService

@UseCase
class RefreshUseCase(
    private val userAdaptor: UserAdaptor,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDomainService: UserDomainService,
    private val refreshTokenAdaptor: RefreshTokenAdaptor,
    private val tokenGenerateHelper: TokenGenerateHelper
) {

    fun execute(refreshToken: String): TokenAndUserResponse {
        val savedRefreshTokenEntity = refreshTokenAdaptor.queryRefreshToken(refreshToken)
        val refreshUserId = jwtTokenProvider.parseRefreshToken(savedRefreshTokenEntity.refreshToken!!)
        val user = userAdaptor.queryUser(refreshUserId)
        // 리프레쉬 시에도 last로그인 정보 업데이트
        userDomainService.loginUser(user.oauthInfo!!)
        return tokenGenerateHelper.execute(user)
    }
}
