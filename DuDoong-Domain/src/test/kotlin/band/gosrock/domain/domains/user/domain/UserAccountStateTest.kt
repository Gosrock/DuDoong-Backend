package band.gosrock.domain.domains.user.domain

import band.gosrock.domain.domains.user.exception.DeletedUserStateChangeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

/** 운영 계정 상태 변경: 탈퇴한 계정은 되돌릴 수 없다 (#762) */
class UserAccountStateTest {

    private fun user() = User(oauthInfo = OauthInfo(OauthProvider.KAKAO, "12345"), profile = Profile(name = "이름"))

    @ParameterizedTest
    @EnumSource(AccountState::class)
    fun `탈퇴한 계정은 어떤 상태로도 바꿀 수 없다 (400)`(newState: AccountState) {
        val user = user().also { it.withDrawUser() }

        val e = assertThrows<DeletedUserStateChangeException> { user.changeAccountState(newState) }
        assertEquals(400, e.getErrorReason().status)
        assertEquals(AccountState.DELETED, user.accountState)
    }

    @Test
    fun `정지한 계정은 정상으로 되돌릴 수 있다`() {
        val user = user()
        user.changeAccountState(AccountState.SUSPENDED)
        user.changeAccountState(AccountState.NORMAL)
        assertEquals(AccountState.NORMAL, user.accountState)
    }
}
