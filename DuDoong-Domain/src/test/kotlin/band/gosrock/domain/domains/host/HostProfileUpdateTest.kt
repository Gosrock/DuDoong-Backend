package band.gosrock.domain.domains.host

import band.gosrock.domain.domains.host.domain.HostProfile
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** #786 — v1 프로필 수정(이름 없음)이 호스트 이름을 null 로 지우던 버그 */
class HostProfileUpdateTest {

    private fun profile() = HostProfile(
        name = "원래이름",
        introduce = "소개",
        contactEmail = "a@example.com",
        contactNumber = "010",
    )

    @Test
    fun `이름 없이 수정하면 기존 이름을 유지하고 나머지는 바꾼다`() {
        val profile = profile()
        profile.updateProfile(HostProfile(introduce = "새 소개", contactEmail = "b@example.com", contactNumber = "011"))
        assertEquals("원래이름", profile.name)
        assertEquals("새 소개", profile.introduce)
        assertEquals("b@example.com", profile.contactEmail)
        assertEquals("011", profile.contactNumber)
    }

    @Test
    fun `이름을 주면 바꾼다`() {
        val profile = profile()
        profile.updateProfile(HostProfile(name = "새이름"))
        assertEquals("새이름", profile.name)
    }
}
