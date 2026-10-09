package band.gosrock.api.auth.service.helper

import band.gosrock.common.exception.InvalidTokenException
import band.gosrock.common.jwt.JwtOIDCProvider
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeyDto
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse
import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 공개키 목록에 없는 kid 는 500 이 아니라 401 (#762 H-4) */
class OauthOIDCHelperTest {

    private val helper = OauthOIDCHelper(JwtOIDCProvider())

    private fun b64(json: String) = Base64.getUrlEncoder().withoutPadding().encodeToString(json.toByteArray())

    private val token = "${b64("""{"alg":"RS256","kid":"unknown-kid"}""")}.${b64("{}")}.sig"

    private fun keys(vararg kids: String) = OIDCPublicKeysResponse().apply {
        keys = kids.map { k -> OIDCPublicKeyDto().apply { kid = k; n = "AQAB"; e = "AQAB" } }
    }

    @Test
    fun `공개키 목록에 kid 가 없으면 401`() {
        val e = assertThrows<InvalidTokenException> {
            helper.getPayloadFromIdToken(token, "https://kauth.kakao.com", setOf("rest-key"), keys("other-kid"))
        }
        assertEquals(401, e.getErrorReason().status)
    }

    @Test
    fun `공개키 목록이 비어 있어도 401`() {
        assertThrows<InvalidTokenException> {
            helper.getPayloadFromIdToken(token, "https://kauth.kakao.com", setOf("rest-key"), OIDCPublicKeysResponse())
        }
    }
}
