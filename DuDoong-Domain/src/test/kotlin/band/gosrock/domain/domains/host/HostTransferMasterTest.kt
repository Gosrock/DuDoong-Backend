package band.gosrock.domain.domains.host

import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.ForbiddenHostException
import band.gosrock.domain.domains.host.exception.NotMasterHostException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils

class HostTransferMasterTest {

    private lateinit var host: Host
    private val masterUserId = 1L
    private val memberUserId = 2L

    @BeforeEach
    fun setUp() {
        host = Host(masterUserId = masterUserId)
        ReflectionTestUtils.setField(host, "id", 100L)

        val masterHostUser = HostUser(host = host, userId = masterUserId, role = HostRole.MASTER)
        ReflectionTestUtils.setField(masterHostUser, "active", true)

        val memberHostUser = HostUser(host = host, userId = memberUserId, role = HostRole.MANAGER)
        ReflectionTestUtils.setField(memberHostUser, "active", true)

        host.hostUsers.addAll(setOf(masterHostUser, memberHostUser))
    }

    @Test
    fun `마스터가 활성 멤버에게 양도하면 성공한다`() {
        host.transferMaster(masterUserId, memberUserId)

        assertEquals(memberUserId, host.masterUserId)
        assertEquals(HostRole.MANAGER, host.getHostUserByUserId(masterUserId).role)
        assertEquals(HostRole.MASTER, host.getHostUserByUserId(memberUserId).role)
    }

    @Test
    fun `마스터가 아닌 유저가 양도를 시도하면 예외가 발생한다`() {
        assertThrows<NotMasterHostException> {
            host.transferMaster(memberUserId, masterUserId)
        }
    }

    @Test
    fun `호스트에 속하지 않은 유저에게 양도하면 예외가 발생한다`() {
        val nonMemberUserId = 999L
        assertThrows<ForbiddenHostException> {
            host.transferMaster(masterUserId, nonMemberUserId)
        }
    }

    @Test
    fun `forceTransferMaster는 권한 검증 없이 양도한다`() {
        host.forceTransferMaster(memberUserId)

        assertEquals(memberUserId, host.masterUserId)
        assertEquals(HostRole.MANAGER, host.getHostUserByUserId(masterUserId).role)
        assertEquals(HostRole.MASTER, host.getHostUserByUserId(memberUserId).role)
    }

    @Test
    fun `forceTransferMaster로 비멤버에게 양도하면 예외가 발생한다`() {
        val nonMemberUserId = 999L
        assertThrows<ForbiddenHostException> {
            host.forceTransferMaster(nonMemberUserId)
        }
    }
}
