package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateHostProfileRequest
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostContact
import band.gosrock.domain.domains.host.domain.HostContactType
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminUpdateHostProfileUseCase - v2 연락처 역방향 동기화")
class AdminUpdateHostProfileUseCaseTest {

    @Mock private lateinit var hostAdaptor: HostAdaptor

    @Mock private lateinit var hostRepository: HostRepository

    @Mock private lateinit var userAdaptor: UserAdaptor

    private lateinit var useCase: AdminUpdateHostProfileUseCase

    private val adminId = 1L
    private val hostId = 10L

    @BeforeEach
    fun setUp() {
        useCase = AdminUpdateHostProfileUseCase(hostAdaptor, hostRepository, AdminAuthValidator(userAdaptor))
        val admin = User(profile = Profile(name = "어드민"))
        ReflectionTestUtils.setField(admin, "id", adminId)
        ReflectionTestUtils.setField(admin, "accountRole", AccountRole.ADMIN)
        `when`(userAdaptor.queryUser(adminId)).thenReturn(admin)
    }

    private fun host(contacts: List<HostContact>): Host =
        Host(masterUserId = 2L, name = "고스락", contactEmail = "old@gosrock.band", contactNumber = "010-1").also {
            ReflectionTestUtils.setField(it, "id", hostId)
            if (contacts.isNotEmpty()) it.replaceContacts(contacts)
            `when`(hostAdaptor.findById(hostId)).thenReturn(it)
        }

    @Test
    fun `v2 연락처가 있으면 어드민이 바꾼 이메일-전화번호가 첫 EMAIL, PHONE 에 반영되고 응답은 v1 필드 그대로다`() {
        val host = host(listOf(HostContact(HostContactType.INSTAGRAM, "@g"), HostContact(HostContactType.EMAIL, "old@gosrock.band")))

        val response = useCase.execute(adminId, hostId, AdminUpdateHostProfileRequest(null, null, "new@gosrock.band", "010-2"))

        assertEquals("new@gosrock.band", response.contactEmail)
        assertEquals("010-2", response.contactNumber)
        assertEquals(
            listOf(HostContactType.INSTAGRAM to "@g", HostContactType.EMAIL to "new@gosrock.band", HostContactType.PHONE to "010-2"),
            host.contacts.map { it.type to it.value },
        )
    }

    @Test
    fun `v2 연락처가 없는 기존 호스트는 연락처 테이블을 만들지 않는다`() {
        val host = host(emptyList())

        useCase.execute(adminId, hostId, AdminUpdateHostProfileRequest(null, null, "new@gosrock.band", null))

        assertTrue(host.contacts.isEmpty())
        assertEquals("new@gosrock.band", host.profile!!.contactEmail)
        assertEquals("010-1", host.profile!!.contactNumber)
    }
}
