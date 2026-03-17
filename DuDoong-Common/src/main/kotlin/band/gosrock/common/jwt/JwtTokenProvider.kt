package band.gosrock.common.jwt

import band.gosrock.common.consts.DuDoongStatic.ACCESS_TOKEN
import band.gosrock.common.consts.DuDoongStatic.ADMIN_AUDIENCE
import band.gosrock.common.consts.DuDoongStatic.MILLI_TO_SECOND
import band.gosrock.common.consts.DuDoongStatic.REFRESH_TOKEN
import band.gosrock.common.consts.DuDoongStatic.TOKEN_ISSUER
import band.gosrock.common.consts.DuDoongStatic.TOKEN_TYPE
import band.gosrock.common.dto.AccessTokenInfo
import band.gosrock.common.exception.ExpiredTokenException
import band.gosrock.common.exception.InvalidTokenException
import band.gosrock.common.exception.RefreshTokenExpiredException
import band.gosrock.common.properties.JwtProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import java.nio.charset.StandardCharsets
import java.util.Date
import javax.crypto.SecretKey
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private fun getJws(token: String): Jws<Claims> =
        try {
            Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException.EXCEPTION
        } catch (e: Exception) {
            throw InvalidTokenException.EXCEPTION
        }

    private fun getSecretKey(): SecretKey =
        Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8))

    private fun buildAccessToken(id: Long, issuedAt: Date, expiresIn: Date, audience: String? = null): String =
        Jwts.builder()
            .issuer(TOKEN_ISSUER)
            .issuedAt(issuedAt)
            .subject(id.toString())
            .claim(TOKEN_TYPE, ACCESS_TOKEN)
            .apply { if (audience != null) audience().add(audience).and() }
            .expiration(expiresIn)
            .signWith(getSecretKey())
            .compact()

    private fun buildRefreshToken(id: Long, issuedAt: Date, expiresIn: Date): String =
        Jwts.builder()
            .issuer(TOKEN_ISSUER)
            .issuedAt(issuedAt)
            .subject(id.toString())
            .claim(TOKEN_TYPE, REFRESH_TOKEN)
            .expiration(expiresIn)
            .signWith(getSecretKey())
            .compact()

    fun generateAccessToken(id: Long): String {
        val issuedAt = Date()
        val accessTokenExpiresIn = Date(issuedAt.time + jwtProperties.accessExp * MILLI_TO_SECOND)
        return buildAccessToken(id, issuedAt, accessTokenExpiresIn)
    }

    fun generateAdminAccessToken(id: Long): String {
        val issuedAt = Date()
        val accessTokenExpiresIn = Date(issuedAt.time + jwtProperties.accessExp * MILLI_TO_SECOND)
        return buildAccessToken(id, issuedAt, accessTokenExpiresIn, audience = ADMIN_AUDIENCE)
    }

    fun generateRefreshToken(id: Long): String {
        val issuedAt = Date()
        val refreshTokenExpiresIn = Date(issuedAt.time + jwtProperties.refreshExp * MILLI_TO_SECOND)
        return buildRefreshToken(id, issuedAt, refreshTokenExpiresIn)
    }

    fun isAccessToken(token: String): Boolean =
        getJws(token).payload.get(TOKEN_TYPE) == ACCESS_TOKEN

    fun isRefreshToken(token: String): Boolean =
        getJws(token).payload.get(TOKEN_TYPE) == REFRESH_TOKEN

    fun parseAccessToken(token: String): AccessTokenInfo {
        if (isAccessToken(token)) {
            val claims = getJws(token).payload
            val audiences = claims.audience
            return AccessTokenInfo(
                userId = claims.subject.toLong(),
                isAdmin = audiences != null && ADMIN_AUDIENCE in audiences,
            )
        }
        throw InvalidTokenException.EXCEPTION
    }

    fun parseRefreshToken(token: String): Long {
        try {
            if (isRefreshToken(token)) {
                val claims = getJws(token).payload
                return claims.subject.toLong()
            }
        } catch (e: ExpiredTokenException) {
            throw RefreshTokenExpiredException.EXCEPTION
        }
        throw InvalidTokenException.EXCEPTION
    }

    fun getRefreshTokenTTlSecond(): Long = jwtProperties.refreshExp

    fun getAccessTokenTTlSecond(): Long = jwtProperties.accessExp
}
