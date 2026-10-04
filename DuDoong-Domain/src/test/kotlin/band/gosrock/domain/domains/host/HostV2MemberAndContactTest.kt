package band.gosrock.domain.domains.host

import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostContact
import band.gosrock.domain.domains.host.domain.HostContactType
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.exception.AlreadyJoinedHostException
import band.gosrock.domain.domains.host.exception.CannotAssignMasterRoleException
import band.gosrock.domain.domains.host.exception.CannotModifyMasterHostRoleException
import band.gosrock.domain.domains.host.exception.CannotRemoveMasterException
import band.gosrock.domain.domains.host.exception.HostUserNotFoundException
import band.gosrock.domain.domains.host.exception.InvalidHostContactException
import band.gosrock.domain.domains.host.exception.ManagerCanManageGuestOnlyException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.test.util.ReflectionTestUtils

class HostV2MemberAndContactTest {

    private lateinit var host: Host
    private val masterId = 1L
    private val managerId = 2L
    private val guestId = 3L
    private val pendingId = 4L

    private fun hostUser(userId: Long, role: HostRole, active: Boolean = true) =
        HostUser(host = host, userId = userId, role = role).also { ReflectionTestUtils.setField(it, "active", active) }

    @BeforeEach
    fun setUp() {
        host = Host(masterUserId = masterId, name = "고스락")
        ReflectionTestUtils.setField(host, "id", 100L)
        host.hostUsers.addAll(
            setOf(
                hostUser(masterId, HostRole.MASTER),
                hostUser(managerId, HostRole.MANAGER),
                hostUser(guestId, HostRole.GUEST),
                hostUser(pendingId, HostRole.GUEST, active = false),
            ),
        )
    }

    @Nested
    inner class AddActiveHostUsers {
        @Test
        fun `마스터는 MANAGER 를 즉시 활성으로 추가한다`() {
            host.addActiveHostUsers(masterId, listOf(HostUser(host = host, userId = 10L, role = HostRole.MANAGER)))

            assertTrue(host.isActiveHostUserId(10L))
            assertEquals(HostRole.MANAGER, host.getActiveRoleOf(10L))
        }

        @Test
        fun `매니저는 GUEST 만 추가할 수 있다`() {
            host.addActiveHostUsers(managerId, listOf(HostUser(host = host, userId = 10L, role = HostRole.GUEST)))
            assertTrue(host.isActiveHostUserId(10L))

            assertThrows<ManagerCanManageGuestOnlyException> {
                host.addActiveHostUsers(managerId, listOf(HostUser(host = host, userId = 11L, role = HostRole.MANAGER)))
            }
            assertFalse(host.hasHostUserId(11L))
        }

        @Test
        fun `MASTER 역할로는 추가할 수 없다`() {
            assertThrows<CannotAssignMasterRoleException> {
                host.addActiveHostUsers(masterId, listOf(HostUser(host = host, userId = 10L, role = HostRole.MASTER)))
            }
        }

        @Test
        fun `이미 멤버이거나 초대 대기 중이거나 요청 내 중복이면 예외`() {
            assertThrows<AlreadyJoinedHostException> {
                host.addActiveHostUsers(masterId, listOf(HostUser(host = host, userId = guestId, role = HostRole.GUEST)))
            }
            assertThrows<AlreadyJoinedHostException> {
                host.addActiveHostUsers(masterId, listOf(HostUser(host = host, userId = pendingId, role = HostRole.GUEST)))
            }
            assertThrows<AlreadyJoinedHostException> {
                host.addActiveHostUsers(
                    masterId,
                    listOf(HostUser(host = host, userId = 10L, role = HostRole.GUEST), HostUser(host = host, userId = 10L, role = HostRole.MANAGER)),
                )
            }
        }
    }

    @Nested
    inner class RoleAndRemove {
        @Test
        fun `역할 변경은 GUEST 와 MANAGER 사이만, 마스터 대상과 비활성 대상은 불가`() {
            host.changeActiveHostUserRole(guestId, HostRole.MANAGER)
            assertEquals(HostRole.MANAGER, host.getActiveRoleOf(guestId))

            assertThrows<CannotAssignMasterRoleException> { host.changeActiveHostUserRole(guestId, HostRole.MASTER) }
            assertThrows<CannotModifyMasterHostRoleException> { host.changeActiveHostUserRole(masterId, HostRole.GUEST) }
            assertThrows<HostUserNotFoundException> { host.changeActiveHostUserRole(pendingId, HostRole.MANAGER) }
        }

        @Test
        fun `마스터는 삭제 불가, 매니저는 GUEST 만 삭제, 마스터는 매니저 삭제 가능`() {
            assertThrows<CannotRemoveMasterException> { host.removeMember(managerId, masterId) }
            assertThrows<CannotRemoveMasterException> { host.removeMember(masterId, masterId) }
            assertThrows<ManagerCanManageGuestOnlyException> { host.removeMember(managerId, managerId) }

            host.removeMember(managerId, guestId)
            assertFalse(host.hasHostUserId(guestId))

            host.removeMember(masterId, managerId)
            assertFalse(host.hasHostUserId(managerId))
        }

        @Test
        fun `초대 대기 멤버도 삭제할 수 있고 없는 멤버는 예외`() {
            host.removeMember(masterId, pendingId)
            assertFalse(host.hasHostUserId(pendingId))
            assertThrows<HostUserNotFoundException> { host.removeMember(masterId, 999L) }
        }

        @Test
        fun `활성 멤버만 조회되고 역할은 활성 멤버만 갖는다`() {
            assertEquals(setOf(masterId, managerId, guestId), host.getActiveHostUsers().map { it.userId }.toSet())
            assertNull(host.getActiveRoleOf(pendingId))
            assertNull(host.getActiveRoleOf(999L))
        }
    }

    @Nested
    inner class Contacts {
        @Test
        fun `연락처를 교체하면 순서가 매겨지고 첫 EMAIL, 첫 PHONE 이 v1 필드에 기록된다`() {
            host.replaceContacts(
                listOf(
                    HostContact(HostContactType.INSTAGRAM, "@gosrock"),
                    HostContact(HostContactType.EMAIL, "a@gosrock.band"),
                    HostContact(HostContactType.PHONE, "010-1234-5678"),
                    HostContact(HostContactType.EMAIL, "b@gosrock.band"),
                ),
            )

            assertEquals(listOf(0, 1, 2, 3), host.contacts.map { it.sortOrder })
            assertTrue(host.contacts.all { it.host === host })
            assertEquals("a@gosrock.band", host.profile!!.contactEmail)
            assertEquals("010-1234-5678", host.profile!!.contactNumber)
            assertEquals(HostContactVo(HostContactType.INSTAGRAM, "@gosrock"), host.displayContacts().first())
        }

        @Test
        fun `교체 후 EMAIL, PHONE 이 없으면 v1 필드는 기존 값을 유지한다`() {
            host.replaceContacts(listOf(HostContact(HostContactType.EMAIL, "a@gosrock.band"), HostContact(HostContactType.PHONE, "010")))
            host.replaceContacts(listOf(HostContact(HostContactType.YOUTUBE, "youtube.com/@gosrock")))

            assertEquals(1, host.contacts.size)
            assertEquals("a@gosrock.band", host.profile!!.contactEmail)
            assertEquals("010", host.profile!!.contactNumber)
        }

        @Test
        fun `v1 프로필 수정은 v2 연락처의 첫 EMAIL, PHONE 값을 갱신한다`() {
            host.replaceContacts(
                listOf(
                    HostContact(HostContactType.EMAIL, "a@gosrock.band"),
                    HostContact(HostContactType.PHONE, "010-1"),
                    HostContact(HostContactType.EMAIL, "b@gosrock.band"),
                ),
            )

            host.updateProfile(HostProfile(name = "고스락", contactEmail = "new@gosrock.band", contactNumber = "010-2"))

            assertEquals(listOf("new@gosrock.band", "010-2", "b@gosrock.band"), host.contacts.map { it.value })
            assertEquals("new@gosrock.band", host.profile!!.contactEmail)
        }

        @Test
        fun `v1 프로필 수정 시 해당 유형이 없으면 끝에 추가하고, 비었거나 최대 개수면 추가하지 않는다`() {
            host.replaceContacts(listOf(HostContact(HostContactType.INSTAGRAM, "@gosrock")))

            host.updateProfile(HostProfile(name = "고스락", contactEmail = "new@gosrock.band", contactNumber = null))

            assertEquals(listOf(HostContactType.INSTAGRAM, HostContactType.EMAIL), host.contacts.map { it.type })
            assertEquals(listOf(0, 1), host.contacts.map { it.sortOrder })
            assertTrue(host.contacts.all { it.host === host })

            host.replaceContacts((1..Host.MAX_CONTACT_COUNT).map { HostContact(HostContactType.ETC, "v$it") })
            host.updateProfile(HostProfile(name = "고스락", contactEmail = "x@gosrock.band", contactNumber = "010"))
            assertEquals(Host.MAX_CONTACT_COUNT, host.contacts.size)
            assertTrue(host.contacts.none { it.type == HostContactType.EMAIL })
        }

        @Test
        fun `v2 연락처가 없는 호스트는 v1 프로필 수정이 연락처 테이블을 만들지 않는다`() {
            host.updateProfile(HostProfile(name = "고스락", contactEmail = "new@gosrock.band", contactNumber = "010"))

            assertTrue(host.contacts.isEmpty())
            assertEquals(HostContactType.PHONE, host.displayContacts().first().type)
        }

        @Test
        fun `0개, 11개, 빈 값, 15자 초과 PHONE 은 예외이고 기존 연락처는 유지된다`() {
            host.replaceContacts(listOf(HostContact(HostContactType.ETC, "keep")))

            assertThrows<InvalidHostContactException> { host.replaceContacts(emptyList()) }
            assertThrows<InvalidHostContactException> {
                host.replaceContacts((1..Host.MAX_CONTACT_COUNT + 1).map { HostContact(HostContactType.ETC, "v$it") })
            }
            assertThrows<InvalidHostContactException> { host.replaceContacts(listOf(HostContact(HostContactType.ETC, " "))) }
            assertThrows<InvalidHostContactException> { host.replaceContacts(listOf(HostContact(HostContactType.PHONE, "0".repeat(16)))) }

            assertEquals(listOf("keep"), host.contacts.map { it.value })
        }

        @Test
        fun `연락처 테이블이 비어 있으면 v1 전화번호, 이메일로 대체 표시한다`() {
            val legacy = Host(masterUserId = masterId, contactEmail = "legacy@gosrock.band", contactNumber = "010-0000-0000")
            assertEquals(
                listOf(
                    HostContactVo(HostContactType.PHONE, "010-0000-0000"),
                    HostContactVo(HostContactType.EMAIL, "legacy@gosrock.band"),
                ),
                legacy.displayContacts(),
            )
            assertEquals(emptyList<HostContactVo>(), Host(masterUserId = masterId).displayContacts())
        }
    }

    @Nested
    inner class ProfileV2 {
        @Test
        fun `null 은 유지, 빈 문자열은 비우고, 값은 변경한다`() {
            host.updateProfileV2(name = null, introduce = "소개", profileImageKey = "p.png", coverImageKey = "c.png")
            host.updateProfileV2(name = "새이름", introduce = null, profileImageKey = null, coverImageKey = "")

            val profile = host.profile!!
            assertEquals("새이름", profile.name)
            assertEquals("소개", profile.introduce)
            assertEquals("p.png", profile.profileImage!!.imageKey)
            assertNull(profile.coverImage!!.imageKey)
        }
    }
}
