package band.gosrock.domain.domains.host.repository

import band.gosrock.domain.DomainIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.user.domain.AccountState
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.repository.UserRepository
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.transaction.annotation.Transactional

/** 마스터 탈퇴 판정 쿼리 (#762 X-4): 탈퇴하지 않은 다른 활성 멤버 또는 진행중·정산중 공연. 트랜잭션 롤백으로 남기지 않는다 */
@DomainIntegrateSpringBootTest
@Transactional
@DisplayName("활성 호스트 마스터 판정 쿼리")
class HostActiveMasterQueryTest {

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var eventRepository: EventRepository

    private val statuses = listOf(EventStatus.OPEN, EventStatus.CALCULATING)

    private fun user(state: AccountState = AccountState.NORMAL): Long =
        userRepository.save(
            User(oauthInfo = OauthInfo(OauthProvider.KAKAO, "q762-${UUID.randomUUID()}")).also {
                ReflectionTestUtils.setField(it, "accountState", state)
            },
        ).id!!

    /** members: (userId, 활성 여부) */
    private fun host(masterId: Long, vararg members: Pair<Long, Boolean>): Long {
        val host = Host(masterUserId = masterId)
        val master = HostUser(host = host, userId = masterId, role = HostRole.MASTER).also { ReflectionTestUtils.setField(it, "active", true) }
        host.hostUsers.add(master)
        members.forEach { (memberId, active) ->
            host.hostUsers.add(HostUser(host = host, userId = memberId, role = HostRole.MANAGER).also { ReflectionTestUtils.setField(it, "active", active) })
        }
        return hostRepository.save(host).id!!
    }

    private fun event(hostId: Long, status: EventStatus) {
        eventRepository.save(
            Event(hostId = hostId, name = "q762", startAt = LocalDateTime.of(2099, 1, 1, 12, 0), runTime = 60).also {
                ReflectionTestUtils.setField(it, "status", status)
            },
        )
    }

    private fun active(userId: Long) = hostRepository.existsActiveHostMasteredBy(userId, statuses)

    @Test
    fun `다른 정상 활성 멤버가 있으면 활성 호스트`() {
        val master = user()
        host(master, user() to true)
        assertTrue(active(master))
    }

    @Test
    fun `정지 멤버도 센다`() {
        val master = user()
        host(master, user(AccountState.SUSPENDED) to true)
        assertTrue(active(master))
    }

    @Test
    fun `탈퇴한 멤버·대기 멤버만 있으면 활성 호스트가 아니다`() {
        val master = user()
        host(master, user(AccountState.DELETED) to true, user() to false)
        assertFalse(active(master))
    }

    @Test
    fun `진행중·정산중 공연이 있으면 혼자여도 활성 호스트`() {
        val open = user()
        event(host(open), EventStatus.OPEN)
        val calculating = user()
        event(host(calculating), EventStatus.CALCULATING)
        assertTrue(active(open))
        assertTrue(active(calculating))
    }

    @Test
    fun `준비중·종료·삭제 공연만 있으면 활성 호스트가 아니다`() {
        val master = user()
        val hostId = host(master)
        listOf(EventStatus.PREPARING, EventStatus.CLOSED, EventStatus.DELETED).forEach { event(hostId, it) }
        assertFalse(active(master))
    }

    @Test
    fun `멤버인 호스트는 세지 않는다 (마스터인 호스트만)`() {
        val master = user()
        val member = user()
        host(master, member to true)
        assertFalse(active(member))
    }
}
