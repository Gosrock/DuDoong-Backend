package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.service.UserDomainService

@UseCase
class LocalDevLoginUseCase(
    private val userDomainService: UserDomainService,
    private val tokenGenerateHelper: TokenGenerateHelper
) {
    fun execute(registerRequest: RegisterRequest): TokenAndUserResponse {
        val oauthInfo = OauthInfo(
            provider = OauthProvider.KAKAO,
            oid = registerRequest.email ?: "anonymous",
        )

        val profile = registerRequest.toProfile()
        val user = userDomainService.upsertUser(profile, oauthInfo)
        return tokenGenerateHelper.execute(user)
    }
}
