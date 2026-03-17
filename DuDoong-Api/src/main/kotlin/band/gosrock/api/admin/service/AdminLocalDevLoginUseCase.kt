package band.gosrock.api.admin.service

import band.gosrock.api.admin.exception.AdminForbiddenException
import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.service.UserDomainService

@UseCase
class AdminLocalDevLoginUseCase(
    private val userDomainService: UserDomainService,
    private val tokenGenerateHelper: TokenGenerateHelper,
) {

    fun execute(registerRequest: RegisterRequest): TokenAndUserResponse {
        val oauthInfo = OauthInfo.builder()
            .provider(OauthProvider.KAKAO)
            .oid("LOCAL_DEV_${registerRequest.email ?: "anonymous"}")
            .build()

        val profile = registerRequest.toProfile()
        val user = userDomainService.upsertUser(profile, oauthInfo)
        if (user.accountRole == AccountRole.USER) {
            throw AdminForbiddenException.EXCEPTION
        }
        return tokenGenerateHelper.execute(user)
    }
}
