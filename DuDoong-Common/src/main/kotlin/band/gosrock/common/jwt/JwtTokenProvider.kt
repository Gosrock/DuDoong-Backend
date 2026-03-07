package band.gosrock.common.jwt

import band.gosrock.common.consts.DuDoongStatic.ACCESS_TOKEN
import band.gosrock.common.consts.DuDoongStatic.MILLI_TO_SECOND
import band.gosrock.common.consts.DuDoongStatic.REFRESH_TOKEN
import band.gosrock.common.consts.DuDoongStatic.TOKEN_ISSUER
import band.gosrock.common.consts.DuDoongStatic.TOKEN_ROLE
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
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets
import java.security.Key
import java.util.Date
import org.springframework.stereotype.Component

@Component
class JwtTokenProvider(
    private val jwtProperties: JwtProperties,
) {
    private fun getJws(token: String): Jws<Claims> =
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSecretKey())
                .build()
                .parseClaimsJws(token)
        } catch (e: ExpiredJwtException) {
            throw ExpiredTokenException.EXCEPTION
        } catch (e: Exception) {
            throw InvalidTokenException.EXCEPTION
        }

    private fun getSecretKey(): Key =
        Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray(StandardCharsets.UTF_8))

    private fun buildAccessToken(id: Long, issuedAt: Date, accessTokenExpiresIn: Date, role: String): String {
        val encodedKey = getSecretKey()
        return Jwts.builder()
            .setIssuer(TOKEN_ISSUER)
            .setIssuedAt(issuedAt)
            .setSubject(id.toString())
            .claim(TOKEN_TYPE, ACCESS_TOKEN)
            .claim(TOKEN_ROLE, role)
            .setExpiration(accessTokenExpiresIn)
            .signWith(encodedKey)
            .compact()
    }

    private fun buildRefreshToken(id: Long, issuedAt: Date, accessTokenExpiresIn: Date): String {
        val encodedKey = getSecretKey()
        return Jwts.builder()
            .setIssuer(TOKEN_ISSUER)
            .setIssuedAt(issuedAt)
            .setSubject(id.toString())
            .claim(TOKEN_TYPE, REFRESH_TOKEN)
            .setExpiration(accessTokenExpiresIn)
            .signWith(encodedKey)
            .compact()
    }

    fun generateAccessToken(id: Long, role: String): String {
        val issuedAt = Date()
        val accessTokenExpiresIn = Date(issuedAt.time + jwtProperties.accessExp * MILLI_TO_SECOND)
        return buildAccessToken(id, issuedAt, accessTokenExpiresIn, role)
    }

    fun generateRefreshToken(id: Long): String {
        val issuedAt = Date()
        val refreshTokenExpiresIn = Date(issuedAt.time + jwtProperties.refreshExp * MILLI_TO_SECOND)
        return buildRefreshToken(id, issuedAt, refreshTokenExpiresIn)
    }

    fun isAccessToken(token: String): Boolean =
        getJws(token).body.get(TOKEN_TYPE) == ACCESS_TOKEN

    fun isRefreshToken(token: String): Boolean =
        getJws(token).body.get(TOKEN_TYPE) == REFRESH_TOKEN

    fun parseAccessToken(token: String): AccessTokenInfo {
        if (isAccessToken(token)) {
            val claims = getJws(token).body
            return AccessTokenInfo(
                userId = claims.subject.toLong(),
                role = claims.get(TOKEN_ROLE, String::class.java),
            )
        }
        throw InvalidTokenException.EXCEPTION
    }

    fun parseRefreshToken(token: String): Long {
        try {
            if (isRefreshToken(token)) {
                val claims = getJws(token).body
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
