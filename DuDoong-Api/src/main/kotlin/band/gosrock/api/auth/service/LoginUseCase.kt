package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.service.UserDomainService
import org.slf4j.LoggerFactory

@UseCase
class LoginUseCase(
    private val kakaoOauthHelper: KakaoOauthHelper,
    private val userDomainService: UserDomainService,
    private val tokenGenerateHelper: TokenGenerateHelper
) {
    private val log = LoggerFactory.getLogger(LoginUseCase::class.java)

    fun execute(idToken: String): TokenAndUserResponse {
        log.info("[LoginUseCase][execute] 로그인 시도")
        val oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken)
        val user = userDomainService.loginUser(oauthInfo)
        log.info("[LoginUseCase][execute] 로그인 성공 userId={}", user.id)
        return tokenGenerateHelper.execute(user)
    }
}
