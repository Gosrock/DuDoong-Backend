package band.gosrock.common.jwt

import band.gosrock.common.consts.DuDoongStatic
import band.gosrock.common.properties.JwtProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.nio.charset.StandardCharsets

class JwtTokenProviderTest {

    private lateinit var jwtTokenProvider: JwtTokenProvider
    private val secretKey = "testkeytestkeytestkeytestkeytestkeytestkeytestkeytestkeytestkey"
    private val accessExp = 3600L
    private val refreshExp = 7200L

    @BeforeEach
    fun setUp() {
        val props = JwtProperties(
            secretKey = secretKey,
            accessExp = accessExp,
            refreshExp = refreshExp,
        )
        jwtTokenProvider = JwtTokenProvider(props)
    }

    private fun parseClaims(token: String) =
        Jwts.parser()
            .verifyWith(Keys.hmacShaKeyFor(secretKey.toByteArray(StandardCharsets.UTF_8)))
            .build()
            .parseSignedClaims(token)
            .payload

    @Nested
    @DisplayName("일반 Access Token")
    inner class GeneralAccessToken {

        @Test
        @DisplayName("JWT payload에 role 클레임이 포함되지 않는다")
        fun `access token does not contain role claim`() {
            val token = jwtTokenProvider.generateAccessToken(1L)
            val claims = parseClaims(token)

            assertFalse(claims.containsKey("role"), "JWT에 role 클레임이 포함되어서는 안 됩니다")
        }

        @Test
        @DisplayName("JWT payload에 userId(subject)만 포함된다")
        fun `access token contains only userId as subject`() {
            val token = jwtTokenProvider.generateAccessToken(42L)
            val claims = parseClaims(token)

            assertEquals("42", claims.subject)
            assertEquals(DuDoongStatic.ACCESS_TOKEN, claims[DuDoongStatic.TOKEN_TYPE])
            assertEquals(DuDoongStatic.TOKEN_ISSUER, claims.issuer)
        }

        @Test
        @DisplayName("일반 토큰의 audience에 admin이 없다")
        fun `general access token has no admin audience`() {
            val token = jwtTokenProvider.generateAccessToken(1L)
            val claims = parseClaims(token)

            val audiences = claims.audience
            assertTrue(audiences == null || DuDoongStatic.ADMIN_AUDIENCE !in audiences,
                "일반 토큰에 admin audience가 포함되어서는 안 됩니다")
        }

        @Test
        @DisplayName("parseAccessToken 시 isAdmin이 false이다")
        fun `parseAccessToken returns isAdmin false for general token`() {
            val token = jwtTokenProvider.generateAccessToken(10L)
            val info = jwtTokenProvider.parseAccessToken(token)

            assertEquals(10L, info.userId)
            assertFalse(info.isAdmin, "일반 토큰의 isAdmin은 false여야 합니다")
        }
    }

    @Nested
    @DisplayName("Admin Access Token")
    inner class AdminAccessToken {

        @Test
        @DisplayName("Admin 토큰에 audience:admin 클레임이 포함된다")
        fun `admin access token contains admin audience`() {
            val token = jwtTokenProvider.generateAdminAccessToken(1L)
            val claims = parseClaims(token)

            val audiences = claims.audience
            assertNotNull(audiences, "Admin 토큰에 audience가 있어야 합니다")
            assertTrue(DuDoongStatic.ADMIN_AUDIENCE in audiences!!,
                "Admin 토큰에 'admin' audience가 포함되어야 합니다")
        }

        @Test
        @DisplayName("Admin 토큰에도 role 클레임이 없다")
        fun `admin access token does not contain role claim`() {
            val token = jwtTokenProvider.generateAdminAccessToken(1L)
            val claims = parseClaims(token)

            assertFalse(claims.containsKey("role"), "Admin JWT에도 role 클레임이 포함되어서는 안 됩니다")
        }

        @Test
        @DisplayName("parseAccessToken 시 isAdmin이 true이다")
        fun `parseAccessToken returns isAdmin true for admin token`() {
            val token = jwtTokenProvider.generateAdminAccessToken(99L)
            val info = jwtTokenProvider.parseAccessToken(token)

            assertEquals(99L, info.userId)
            assertTrue(info.isAdmin, "Admin 토큰의 isAdmin은 true여야 합니다")
        }

        @Test
        @DisplayName("Admin 토큰과 일반 토큰의 userId는 동일하게 파싱된다")
        fun `admin and general tokens parse same userId`() {
            val generalToken = jwtTokenProvider.generateAccessToken(55L)
            val adminToken = jwtTokenProvider.generateAdminAccessToken(55L)

            val generalInfo = jwtTokenProvider.parseAccessToken(generalToken)
            val adminInfo = jwtTokenProvider.parseAccessToken(adminToken)

            assertEquals(generalInfo.userId, adminInfo.userId)
            assertFalse(generalInfo.isAdmin)
            assertTrue(adminInfo.isAdmin)
        }
    }

    @Nested
    @DisplayName("Refresh Token")
    inner class RefreshToken {

        @Test
        @DisplayName("Refresh 토큰은 userId만 포함한다")
        fun `refresh token contains only userId`() {
            val token = jwtTokenProvider.generateRefreshToken(7L)
            val claims = parseClaims(token)

            assertEquals("7", claims.subject)
            assertEquals(DuDoongStatic.REFRESH_TOKEN, claims[DuDoongStatic.TOKEN_TYPE])
            assertFalse(claims.containsKey("role"))
        }

        @Test
        @DisplayName("parseRefreshToken으로 userId를 추출할 수 있다")
        fun `parseRefreshToken extracts userId`() {
            val token = jwtTokenProvider.generateRefreshToken(33L)
            val userId = jwtTokenProvider.parseRefreshToken(token)

            assertEquals(33L, userId)
        }
    }

    @Nested
    @DisplayName("토큰 구분")
    inner class TokenTypeDistinction {

        @Test
        @DisplayName("Access Token은 isAccessToken=true, isRefreshToken=false")
        fun `access token type check`() {
            val token = jwtTokenProvider.generateAccessToken(1L)

            assertTrue(jwtTokenProvider.isAccessToken(token))
            assertFalse(jwtTokenProvider.isRefreshToken(token))
        }

        @Test
        @DisplayName("Admin Access Token도 isAccessToken=true")
        fun `admin access token is also access token`() {
            val token = jwtTokenProvider.generateAdminAccessToken(1L)

            assertTrue(jwtTokenProvider.isAccessToken(token))
            assertFalse(jwtTokenProvider.isRefreshToken(token))
        }

        @Test
        @DisplayName("Refresh Token은 isRefreshToken=true, isAccessToken=false")
        fun `refresh token type check`() {
            val token = jwtTokenProvider.generateRefreshToken(1L)

            assertTrue(jwtTokenProvider.isRefreshToken(token))
            assertFalse(jwtTokenProvider.isAccessToken(token))
        }
    }
}
