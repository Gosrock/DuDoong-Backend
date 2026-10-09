package band.gosrock.domain.domains.user.service

import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.exception.HostMasterCannotWithdrawException
import band.gosrock.domain.domains.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

/** 회원 탈퇴·운영 상태 변경의 호스트 마스터 검사 (#762 X-4). 판정 쿼리는 HostActiveMasterQueryTest */
@ExtendWith(MockitoExtension::class)
@DisplayName("UserDomainService 탈퇴 — 호스트 마스터 검사")
class UserDomainServiceWithdrawTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var userAdaptor: UserAdaptor
    @Mock private lateinit var hostAdaptor: HostAdaptor

    private lateinit var service: UserDomainService
    private lateinit var user: User
    private val userId = 1L
    private val activeStatuses = listOf(EventStatus.OPEN, EventStatus.CALCULATING)

    @BeforeEach
    fun setUp() {
        service = UserDomainService(userRepository, userAdaptor, hostAdaptor)
        user = User(oauthInfo = OauthInfo(OauthProvider.KAKAO, "12345"))
        ReflectionTestUtils.setField(user, "id", userId)
        `when`(userAdaptor.queryUser(userId)).thenReturn(user)
    }

    private fun activeMaster(active: Boolean) {
        `when`(hostAdaptor.existsActiveHostMasteredBy(userId, activeStatuses)).thenReturn(active)
    }

    @Test
    fun `활성 호스트의 마스터는 탈퇴할 수 없다`() {
        activeMaster(true)

        assertThrows<HostMasterCannotWithdrawException> { service.withDrawUser(userId) }
        assertEquals(AccountState.NORMAL, user.accountState)
    }

    @Test
    fun `활성 호스트의 마스터가 아니면 탈퇴하고 oid 를 돌려준다`() {
        activeMaster(false)

        assertEquals("12345", service.withDrawUser(userId))
        assertEquals(AccountState.DELETED, user.accountState)
    }

    @Test
    fun `운영 탈퇴(DELETED)도 같은 마스터 검사를 거친다`() {
        activeMaster(true)

        assertThrows<HostMasterCannotWithdrawException> { service.changeAccountStateByAdmin(userId, AccountState.DELETED) }
        assertEquals(AccountState.NORMAL, user.accountState)
    }

    @Test
    fun `운영 정지는 마스터 검사 없이 상태만 바꾼다`() {
        lenient().`when`(hostAdaptor.existsActiveHostMasteredBy(userId, activeStatuses)).thenReturn(true)

        assertEquals(AccountState.SUSPENDED, service.changeAccountStateByAdmin(userId, AccountState.SUSPENDED).accountState)
    }
}
