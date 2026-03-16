package band.gosrock.api.admin.service

import band.gosrock.api.admin.exception.AdminForbiddenException
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.service.UserDomainService

@UseCase
class AdminLoginUseCase(
    private val kakaoOauthHelper: KakaoOauthHelper,
    private val userDomainService: UserDomainService,
    private val tokenGenerateHelper: TokenGenerateHelper,
) {

    fun execute(idToken: String): TokenAndUserResponse {
        val oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken)
        val user = userDomainService.loginUser(oauthInfo)
        if (user.accountRole == AccountRole.USER) {
            throw AdminForbiddenException.EXCEPTION
        }
        return tokenGenerateHelper.execute(user)
    }
}
