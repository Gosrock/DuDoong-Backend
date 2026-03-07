package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConstructorBinding
@ConfigurationProperties("oauth")
class OauthProperties(
    private val kakao: OAuthSecret,
) {
    fun getKakaoAdminKey(): String = kakao.adminKey
    fun getKakaoBaseUrl(): String = kakao.baseUrl
    fun getKakaoClientId(): String = kakao.clientId
    fun getKakaoRedirectUrl(): String = kakao.redirectUrl
    fun getKakaoClientSecret(): String = kakao.clientSecret
    fun getKakaoAppId(): String = kakao.appId

    data class OAuthSecret(
        var baseUrl: String = "",
        var clientId: String = "",
        var clientSecret: String = "",
        var redirectUrl: String = "",
        var appId: String = "",
        var adminKey: String = "",
    )
}
