package band.gosrock.api.auth.service

import band.gosrock.api.auth.model.dto.response.OauthUserInfoResponse
import band.gosrock.api.auth.service.helper.KakaoOauthHelper
import band.gosrock.common.annotation.UseCase

@UseCase
class OauthUserInfoUseCase(
    private val kakaoOauthHelper: KakaoOauthHelper
) {

    fun execute(accessToken: String): OauthUserInfoResponse {
        val oauthUserInfo = kakaoOauthHelper.getUserInfo(accessToken)
        return OauthUserInfoResponse.from(oauthUserInfo)
    }
}
