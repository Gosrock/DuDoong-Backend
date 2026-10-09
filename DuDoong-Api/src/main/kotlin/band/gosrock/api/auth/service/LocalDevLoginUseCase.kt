package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.common.consts.DuDoongStatic.LOCAL_OID_PREFIX
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
            oid = LOCAL_OID_PREFIX + (registerRequest.email ?: "anonymous"),
        )

        val profile = registerRequest.toProfile()
        userDomainService.upsertUser(profile, oauthInfo)
        // 카카오 로그인과 같은 계정 상태 검사 (정지·탈퇴 계정은 403)
        val user = userDomainService.loginUser(oauthInfo)
        return tokenGenerateHelper.execute(user)
    }
}
