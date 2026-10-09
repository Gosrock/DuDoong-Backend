package band.gosrock.common.jwt

import band.gosrock.common.exception.InvalidTokenException
import io.jsonwebtoken.Jwts
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 카카오 id_token 검증: 서명·만료 + iss·aud (#762 H-4) */
@DisplayName("JwtOIDCProvider")
class JwtOIDCProviderTest {

    private val provider = JwtOIDCProvider()
    private val keyPair: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
    private val publicKey = keyPair.public as RSAPublicKey
    private val modulus = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.modulus.toByteArray())
    private val exponent = Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.publicExponent.toByteArray())

    private val iss = "https://kauth.kakao.com"
    private val audiences = setOf("rest-key", "native-key")

    private fun idToken(iss: String = this.iss, aud: String = "rest-key", kid: String = "kid-1"): String =
        Jwts.builder()
            .header().keyId(kid).and()
            .issuer(iss)
            .audience().add(aud).and()
            .subject("12345")
            .claim("email", "a@b.com")
            .expiration(Date(System.currentTimeMillis() + 60_000))
            .signWith(keyPair.private)
            .compact()

    private fun body(token: String) = provider.getOIDCTokenBody(token, modulus, exponent, iss, audiences)

    @Test
    fun `iss 와 aud 가 맞으면 통과한다`() {
        assertEquals("12345", body(idToken()).sub)
    }

    @Test
    fun `aud 가 허용 목록의 다른 값이어도 통과한다`() {
        assertEquals("native-key", body(idToken(aud = "native-key")).aud)
    }

    @Test
    fun `aud 가 다르면 거부한다`() {
        val e = assertThrows<InvalidTokenException> { body(idToken(aud = "other-app")) }
        assertEquals(401, e.getErrorReason().status)
    }

    @Test
    fun `iss 가 다르면 거부한다`() {
        val e = assertThrows<InvalidTokenException> { body(idToken(iss = "https://evil.example.com")) }
        assertEquals(401, e.getErrorReason().status)
    }

    @Test
    fun `헤더의 kid 를 읽는다`() {
        assertEquals("kid-1", provider.getKidFromUnsignedTokenHeader(idToken(kid = "kid-1")))
    }

    @Test
    fun `헤더가 base64 가 아니면 401`() {
        val e = assertThrows<InvalidTokenException> { provider.getKidFromUnsignedTokenHeader("@@@.e30.sig") }
        assertSame(InvalidTokenException.EXCEPTION, e)
    }
}
