package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse
import band.gosrock.api.auth.model.dto.response.OauthTokenResponse
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.api.auth.service.helper.TokenGenerateHelper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.service.UserDomainService
import org.slf4j.LoggerFactory

@UseCase
class RegisterUseCase(
    private val kakaoOauthHelper: KakaoOauthHelper,
    private val userDomainService: UserDomainService,
    private val tokenGenerateHelper: TokenGenerateHelper
) {
    private val log = LoggerFactory.getLogger(RegisterUseCase::class.java)

    fun getKaKaoOauthLinkTest(): OauthLoginLinkResponse =
        OauthLoginLinkResponse(kakaoOauthHelper.getKaKaoOauthLinkTest())

    fun getKaKaoOauthLink(referer: String): OauthLoginLinkResponse =
        OauthLoginLinkResponse(kakaoOauthHelper.getKaKaoOauthLink(referer))

    fun upsertKakaoOauthUser(code: String): TokenAndUserResponse {
        val oauthAccessToken = kakaoOauthHelper.getOauthTokenTest(code).accessToken
        val oauthUserInfo = kakaoOauthHelper.getUserInfo(oauthAccessToken!!)

        val profile = oauthUserInfo.toProfile()
        val user = userDomainService.upsertUser(profile, oauthUserInfo.toOauthInfo())

        return tokenGenerateHelper.execute(user)
    }

    fun checkAvailableRegister(idToken: String): AvailableRegisterResponse {
        val oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken)
        return AvailableRegisterResponse(userDomainService.checkUserCanRegister(oauthInfo))
    }

    fun registerUserByOCIDToken(
        idToken: String,
        registerUserRequest: RegisterRequest
    ): TokenAndUserResponse {
        log.info("[RegisterUseCase][registerUserByOCIDToken] 회원가입")
        val oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken)
        val user = userDomainService.registerUser(
            registerUserRequest.toProfile(),
            oauthInfo,
            registerUserRequest.marketingAgree
        )
        log.info("[RegisterUseCase][registerUserByOCIDToken] 회원가입 완료 userId={}", user.id)
        return tokenGenerateHelper.execute(user)
    }

    fun getCredentialFromKaKao(code: String, referer: String): OauthTokenResponse =
        OauthTokenResponse.from(kakaoOauthHelper.getOauthToken(code, referer))

    fun getCredentialFromKaKaoTest(code: String): OauthTokenResponse =
        OauthTokenResponse.from(kakaoOauthHelper.getOauthTokenTest(code))
}
