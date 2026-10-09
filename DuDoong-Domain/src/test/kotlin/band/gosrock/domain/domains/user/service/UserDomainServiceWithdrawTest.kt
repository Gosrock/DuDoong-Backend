package band.gosrock.domain.domains.user.service

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.test.util.ReflectionTestUtils

/** 회원 탈퇴·운영 상태 변경의 호스트 마스터 검사 (#762 X-4) */
@ExtendWith(MockitoExtension::class)
@DisplayName("UserDomainService 탈퇴 — 호스트 마스터 검사")
class UserDomainServiceWithdrawTest {

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var userAdaptor: UserAdaptor
    @Mock private lateinit var hostAdaptor: HostAdaptor
    @Mock private lateinit var eventAdaptor: EventAdaptor

    private lateinit var service: UserDomainService
    private lateinit var user: User
    private val userId = 1L
    private val hostId = 100L
    private val activeStatuses = listOf(EventStatus.OPEN, EventStatus.CALCULATING)
    private val firstOne = PageRequest.of(0, 1)

    @BeforeEach
    fun setUp() {
        service = UserDomainService(userRepository, userAdaptor, hostAdaptor, eventAdaptor)
        user = User(oauthInfo = OauthInfo(OauthProvider.KAKAO, "12345"))
        ReflectionTestUtils.setField(user, "id", userId)
        `when`(userAdaptor.queryUser(userId)).thenReturn(user)
    }

    private fun host(vararg members: Pair<Long, Boolean>): Host {
        val host = Host(masterUserId = userId)
        ReflectionTestUtils.setField(host, "id", hostId)
        val master = HostUser(host = host, userId = userId, role = HostRole.MASTER)
        ReflectionTestUtils.setField(master, "active", true)
        host.hostUsers.add(master)
        members.forEach { (memberId, active) ->
            val member = HostUser(host = host, userId = memberId, role = HostRole.MANAGER)
            ReflectionTestUtils.setField(member, "active", active)
            host.hostUsers.add(member)
        }
        return host
    }

    private fun events(count: Int): Page<Event> = PageImpl(List(count) { mock(Event::class.java) })

    @Test
    fun `다른 활성 멤버가 있는 호스트의 마스터는 탈퇴할 수 없다`() {
        `when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(listOf(host(2L to true)))

        assertThrows<HostMasterCannotWithdrawException> { service.withDrawUser(userId) }
        assertEquals(AccountState.NORMAL, user.accountState)
    }

    @Test
    fun `진행중·정산중 공연이 있는 호스트의 마스터는 혼자여도 탈퇴할 수 없다`() {
        `when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(listOf(host()))
        `when`(eventAdaptor.findAllByHostIdAndStatusIn(hostId, activeStatuses, firstOne)).thenReturn(events(1))

        assertThrows<HostMasterCannotWithdrawException> { service.withDrawUser(userId) }
        assertEquals(AccountState.NORMAL, user.accountState)
    }

    @Test
    fun `혼자이고 진행 중인 공연이 없는 호스트의 마스터는 탈퇴할 수 있다 (대기 멤버는 세지 않는다)`() {
        `when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(listOf(host(3L to false)))
        `when`(eventAdaptor.findAllByHostIdAndStatusIn(hostId, activeStatuses, firstOne)).thenReturn(events(0))

        val oid = service.withDrawUser(userId)

        assertEquals("12345", oid)
        assertEquals(AccountState.DELETED, user.accountState)
    }

    @Test
    fun `마스터인 호스트가 없으면 탈퇴할 수 있다`() {
        `when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(emptyList())

        service.withDrawUser(userId)

        assertEquals(AccountState.DELETED, user.accountState)
    }

    @Test
    fun `운영 탈퇴(DELETED)도 같은 마스터 검사를 거친다`() {
        `when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(listOf(host(2L to true)))

        assertThrows<HostMasterCannotWithdrawException> { service.changeAccountStateByAdmin(userId, AccountState.DELETED) }
        assertEquals(AccountState.NORMAL, user.accountState)
    }

    @Test
    fun `운영 정지는 마스터 검사 없이 상태만 바꾼다`() {
        lenient().`when`(hostAdaptor.findAllByMasterUserId(userId)).thenReturn(listOf(host(2L to true)))

        val changed = service.changeAccountStateByAdmin(userId, AccountState.SUSPENDED)

        assertEquals(AccountState.SUSPENDED, changed.accountState)
    }
}
