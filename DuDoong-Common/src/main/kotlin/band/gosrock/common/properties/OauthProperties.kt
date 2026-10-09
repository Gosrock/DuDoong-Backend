package band.gosrock.common.properties

import org.springframework.boot.context.properties.ConfigurationProperties

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

    /** 카카오 id_token 의 iss. 인가 링크에 쓰는 base-url(https://kauth.kakao.com)과 같다 */
    fun getKakaoIssuer(): String = kakao.baseUrl.trimEnd('/')

    /**
     * 카카오 id_token 의 aud 허용 목록 (aud = 토큰을 받은 카카오 앱 키).
     * app-id(쉼표로 여러 개) + client-id(이 서버가 인가 코드를 토큰으로 바꿀 때 쓰는 REST 키)
     */
    fun getKakaoAudiences(): Set<String> =
        (kakao.appId.split(",") + kakao.clientId).map { it.trim() }.filter { it.isNotEmpty() }.toSet()

    data class OAuthSecret(
        var baseUrl: String = "",
        var clientId: String = "",
        var clientSecret: String = "",
        var redirectUrl: String = "",
        var appId: String = "",
        var adminKey: String = "",
    )
}
