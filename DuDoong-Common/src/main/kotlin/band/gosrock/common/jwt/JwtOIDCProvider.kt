package band.gosrock.common.jwt

import band.gosrock.common.dto.OIDCDecodePayload
import band.gosrock.common.exception.ExpiredTokenException
import band.gosrock.common.exception.InvalidTokenException
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import java.math.BigInteger
import java.security.Key
import java.security.KeyFactory
import java.security.spec.RSAPublicKeySpec
import java.util.Base64
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class JwtOIDCProvider {
    private val log = LoggerFactory.getLogger(JwtOIDCProvider::class.java)
    private val KID = "kid"

    fun getKidFromUnsignedTokenHeader(token: String): String {
        val unsignedToken = getUnsignedToken(token)
        val splitToken = unsignedToken.split(".")
        val headerJson = runCatching { String(Base64.getUrlDecoder().decode(splitToken[0])) }
            .getOrElse { throw InvalidTokenException.EXCEPTION }
        // Parse kid from header manually since JJWT 0.12 removed unsigned JWT parsing
        val kidRegex = """"kid"\s*:\s*"([^"]+)"""".toRegex()
        val match = kidRegex.find(headerJson) ?: throw InvalidTokenException.EXCEPTION
        return match.groupValues[1]
    }

    private fun getUnsignedToken(token: String): String {
        val splitToken = token.split(".")
        if (splitToken.size != 3) throw InvalidTokenException.EXCEPTION
        return "${splitToken[0]}.${splitToken[1]}."
    }

    /**
     * 서명·만료에 더해 발급자(iss)와 대상(aud)을 검증한다. aud 는 허용 목록 중 하나라도 들어 있으면 통과한다 (카카오 앱 키가 여러 개일 수 있다)
     */
    fun getOIDCTokenJws(token: String, modulus: String, exponent: String, iss: String, audiences: Set<String>): Jws<Claims> {
        val jws = try {
            Jwts.parser()
                .verifyWith(getRSAPublicKey(modulus, exponent) as java.security.PublicKey)
                .requireIssuer(iss)
                .build()
                .parseSignedClaims(token)
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException.EXCEPTION
        } catch (e: Exception) {
            log.error(e.toString())
            throw InvalidTokenException.EXCEPTION
        }
        if (jws.payload.audience.orEmpty().none { it in audiences }) {
            log.error("OIDC id_token aud 불일치")
            throw InvalidTokenException.EXCEPTION
        }
        return jws
    }

    fun getOIDCTokenBody(token: String, modulus: String, exponent: String, iss: String, audiences: Set<String>): OIDCDecodePayload {
        val body = getOIDCTokenJws(token, modulus, exponent, iss, audiences).payload
        return OIDCDecodePayload(
            iss = body.issuer,
            aud = body.audience.firstOrNull() ?: "",
            sub = body.subject,
            email = body.get("email", String::class.java),
        )
    }

    private fun getRSAPublicKey(modulus: String, exponent: String): Key {
        val keyFactory = KeyFactory.getInstance("RSA")
        val decodeN = Base64.getUrlDecoder().decode(modulus)
        val decodeE = Base64.getUrlDecoder().decode(exponent)
        val n = BigInteger(1, decodeN)
        val e = BigInteger(1, decodeE)
        val keySpec = RSAPublicKeySpec(n, e)
        return keyFactory.generatePublic(keySpec)
    }
}
