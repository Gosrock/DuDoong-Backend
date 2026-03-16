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

    fun getKidFromUnsignedTokenHeader(token: String, iss: String, aud: String): String {
        val unsignedToken = getUnsignedToken(token)
        val splitToken = unsignedToken.split(".")
        val headerJson = String(Base64.getUrlDecoder().decode(splitToken[0]))
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

    fun getOIDCTokenJws(token: String, modulus: String, exponent: String): Jws<Claims> =
        try {
            Jwts.parser()
                .verifyWith(getRSAPublicKey(modulus, exponent) as java.security.PublicKey)
                .build()
                .parseSignedClaims(token)
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException.EXCEPTION
        } catch (e: Exception) {
            log.error(e.toString())
            throw InvalidTokenException.EXCEPTION
        }

    fun getOIDCTokenBody(token: String, modulus: String, exponent: String): OIDCDecodePayload {
        val body = getOIDCTokenJws(token, modulus, exponent).payload
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
