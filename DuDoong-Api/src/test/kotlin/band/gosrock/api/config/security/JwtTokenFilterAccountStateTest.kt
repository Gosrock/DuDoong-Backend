package band.gosrock.api.config.security

import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.dto.AccessTokenInfo
import band.gosrock.common.jwt.JwtTokenProvider
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** 정지·탈퇴 계정의 기존 access 토큰으로는 인증되지 않는다 (#762 M-2). 경로별 응답은 E2E test_57 */
class JwtTokenFilterAccountStateTest {

    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val userAdaptor = mock(UserAdaptor::class.java)
    private val filter = JwtTokenFilter(jwtTokenProvider, userAdaptor, mock(CookieHelper::class.java))
    private val user = User()

    @BeforeEach
    fun setUp() {
        ReflectionTestUtils.setField(user, "id", 1L)
        `when`(jwtTokenProvider.parseAccessToken("token")).thenReturn(AccessTokenInfo(userId = 1L))
        `when`(userAdaptor.queryUser(1L)).thenReturn(user)
    }

    @Test
    fun `정상 계정은 인증된다`() {
        assertEquals("1", filter.getAuthentication("token")!!.name)
    }

    @ParameterizedTest
    @EnumSource(value = AccountState::class, names = ["NORMAL"], mode = EnumSource.Mode.EXCLUDE)
    fun `정상이 아닌 계정의 토큰은 인증하지 않는다 (익명 처리)`(state: AccountState) {
        ReflectionTestUtils.setField(user, "accountState", state)

        assertNull(filter.getAuthentication("token"))
    }
}
