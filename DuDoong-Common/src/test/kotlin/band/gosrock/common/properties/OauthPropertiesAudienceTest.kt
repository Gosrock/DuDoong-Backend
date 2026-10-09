package band.gosrock.common.properties

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** 카카오 id_token iss·aud 허용값 (#762 H-4) */
class OauthPropertiesAudienceTest {

    private fun props(baseUrl: String = "https://kauth.kakao.com", clientId: String = "rest-key", appId: String = "") =
        OauthProperties(OauthProperties.OAuthSecret(baseUrl = baseUrl, clientId = clientId, appId = appId))

    @Test
    fun `aud 허용 목록은 app-id(쉼표 구분)와 client-id 를 합친다`() {
        assertEquals(setOf("native-key", "js-key", "rest-key"), props(appId = "native-key, js-key").getKakaoAudiences())
    }

    @Test
    fun `빈 값은 허용 목록에 넣지 않는다`() {
        assertEquals(setOf("rest-key"), props(appId = " , ").getKakaoAudiences())
        assertEquals(emptySet<String>(), props(clientId = "", appId = "").getKakaoAudiences())
    }

    @Test
    fun `iss 는 base-url 의 끝 슬래시를 뗀다`() {
        assertEquals("https://kauth.kakao.com", props(baseUrl = "https://kauth.kakao.com/").getKakaoIssuer())
    }
}
