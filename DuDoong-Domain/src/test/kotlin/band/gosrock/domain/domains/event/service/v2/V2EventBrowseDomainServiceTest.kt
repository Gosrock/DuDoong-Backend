package band.gosrock.domain.domains.event.service.v2

import band.gosrock.domain.common.vo.HostContactVo
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventContact
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.exception.EventNotFoundException
import band.gosrock.domain.domains.event.exception.InvalidEventTagException
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostContact
import band.gosrock.domain.domains.host.domain.HostContactType
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.host.service.v2.V2HostDomainService
import band.gosrock.domain.domains.tag.adaptor.TagAdaptor
import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagCategory
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.springframework.test.util.ReflectionTestUtils

/** v2 사용자 앱 공연 탐색 규칙 (#716): 공개 여부, 태그 필터 묶음, 문의처 대체. 표시 상태는 V2EventDisplayRuleTest */
class V2EventBrowseDomainServiceTest {

    private fun tag(id: Long, category: TagCategory): Tag =
        Tag(category = category, name = "t$id", sortOrder = 0).also { ReflectionTestUtils.setField(it, "id", id) }

    // id 1~3 공연구분, 10~11 장르, 20 장소, 30 팀 (조회 결과 순서는 섞어서 돌려준다)
    private val allTags = listOf(
        tag(30, TagCategory.TEAM), tag(11, TagCategory.GENRE), tag(2, TagCategory.EVENT_TYPE),
        tag(20, TagCategory.AREA), tag(1, TagCategory.EVENT_TYPE), tag(10, TagCategory.GENRE), tag(3, TagCategory.EVENT_TYPE),
    )

    private val tagAdaptor: TagAdaptor = mock(TagAdaptor::class.java) { inv ->
        val ids = (inv.arguments[0] as Collection<*>).toSet()
        allTags.filter { it.id in ids }
    }

    private val service = V2EventBrowseDomainService(
        tagAdaptor = tagAdaptor,
        v2HostDomainService = V2HostDomainService(mock(HostRepository::class.java)),
    )

    private val now: LocalDateTime = LocalDateTime.of(2030, 1, 1, 12, 0)

    private fun event(status: EventStatus, startAt: LocalDateTime? = now.plusDays(1)): Event =
        Event(hostId = 1L, name = "공연", startAt = startAt, runTime = 120).also { ReflectionTestUtils.setField(it, "status", status) }

    @Nested
    inner class ValidatePublic {
        @Test
        fun `등록·정산중·지난공연은 공개`() {
            listOf(EventStatus.OPEN, EventStatus.CALCULATING, EventStatus.CLOSED).forEach { assertDoesNotThrow { service.validatePublic(event(it)) } }
        }

        @Test
        fun `준비중·삭제는 404 (존재 숨김)`() {
            listOf(EventStatus.PREPARING, EventStatus.DELETED).forEach {
                assertEquals(EventNotFoundException.EXCEPTION, assertThrows<Exception> { service.validatePublic(event(it)) })
            }
        }
    }

    @Nested
    inner class TagFilterGroups {
        @Test
        fun `분류별로 묶고 분류 순서(EVENT_TYPE GENRE AREA TEAM), 묶음 안은 id 순`() {
            assertEquals(
                listOf(listOf(1L, 3L), listOf(10L, 11L), listOf(20L), listOf(30L)),
                service.tagFilterGroups(listOf(30, 11, 3, 20, 10, 1)),
            )
        }

        @Test
        fun `같은 분류만이면 묶음 하나 (OR), 중복 id 는 하나로`() {
            assertEquals(listOf(listOf(1L, 2L)), service.tagFilterGroups(listOf(2, 1, 2)))
        }

        @Test
        fun `비어 있으면 필터 없음`() {
            assertEquals(emptyList<List<Long>>(), service.tagFilterGroups(emptyList()))
        }

        @Test
        fun `없는 태그 id 가 있으면 400`() {
            assertEquals(InvalidEventTagException.EXCEPTION, assertThrows<Exception> { service.tagFilterGroups(listOf(1, 999)) })
        }
    }

    @Nested
    inner class DisplayContacts {
        private fun host(v1Phone: String? = null, v1Email: String? = null, v2: List<HostContact> = emptyList()): Host =
            Host(masterUserId = 1L, name = "고스락", contactNumber = v1Phone, contactEmail = v1Email).also { it.contacts.addAll(v2) }

        @Test
        fun `공연 문의처가 있으면 그대로 (순서 유지), 호스트 연락처는 쓰지 않는다`() {
            val event = event(EventStatus.OPEN).also {
                it.contacts.add(EventContact(type = HostContactType.INSTAGRAM, value = "@gosrock"))
                it.contacts.add(EventContact(type = HostContactType.PHONE, value = "010-1111-2222"))
            }
            assertEquals(
                listOf(HostContactVo(HostContactType.INSTAGRAM, "@gosrock"), HostContactVo(HostContactType.PHONE, "010-1111-2222")),
                service.displayContacts(event, host(v1Email = "v1@gosrock.band")),
            )
        }

        @Test
        fun `공연 문의처가 없으면 호스트 v2 연락처`() {
            val v2 = HostContact(type = HostContactType.EMAIL, value = "v2@gosrock.band")
            assertEquals(
                listOf(HostContactVo(HostContactType.EMAIL, "v2@gosrock.band")),
                service.displayContacts(event(EventStatus.OPEN), host(v1Phone = "010-0000-0000", v2 = listOf(v2))),
            )
        }

        @Test
        fun `공연 문의처·호스트 v2 연락처가 모두 없으면 v1 전화·이메일`() {
            assertEquals(
                listOf(HostContactVo(HostContactType.PHONE, "010-0000-0000"), HostContactVo(HostContactType.EMAIL, "v1@gosrock.band")),
                service.displayContacts(event(EventStatus.OPEN), host(v1Phone = "010-0000-0000", v1Email = "v1@gosrock.band")),
            )
            assertEquals(emptyList<HostContactVo>(), service.displayContacts(event(EventStatus.OPEN), host()))
        }
    }
}
