package band.gosrock.api.v1security

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.support.V2ImageKeys
import band.gosrock.api.v2.ticket.V2TicketApiTestSupport
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * v1 호스트·티켓 API 권한 / 응답 최소화 (#761).
 * Security 필터와 @HostRolesAllowed AOP 가 실제로 동작하는 MockMvc 통합 테스트. 픽스처는 v2 티켓 테스트와 같다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v1 호스트 / 티켓 권한 (#761)")
class V1HostTicketAuthzTest : V2TicketApiTestSupport() {

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    private val phone = "010-7761-4321"

    private fun userWithPhone(name: String): User =
        userRepository.save(
            User(
                profile = Profile(name = name, email = "v1authz-${UUID.randomUUID()}@test.com", phoneNumber = phone, profileImage = null),
                oauthInfo = OauthInfo(OauthProvider.KAKAO, "v1authz-${UUID.randomUUID()}"),
            ),
        )

    private fun setSlackUrl(hostId: Long, url: String) {
        val host = hostRepository.findById(hostId).get()
        host.slackUrl = url
        hostRepository.save(host)
    }

    private fun addPendingMember(hostId: Long, user: User, role: HostRole) {
        val host = hostRepository.findById(hostId).get()
        host.hostUsers.add(HostUser(host = host, userId = user.id, role = role))
        hostRepository.save(host)
    }

    private fun ResultActionsDsl.expectCode(status: Int, code: String): ResultActionsDsl = andExpect {
        status { isEqualTo(status) }
        jsonPath("$.code") { value(code) }
    }

    private fun ResultActionsDsl.raw(): String = andReturn().response.getContentAsString(Charsets.UTF_8)

    private fun getHost(requester: User, hostId: Long): ResultActionsDsl =
        mockMvc.get("/api/v1/hosts/$hostId") { with(auth(requester)) }

    private fun invite(requester: User, hostId: Long, email: String, role: String): ResultActionsDsl =
        mockMvc.post("/api/v1/hosts/$hostId/invite") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("email" to email, "role" to role))
        }

    private fun changeRole(requester: User, hostId: Long, targetUserId: Long, role: String): ResultActionsDsl =
        mockMvc.patch("/api/v1/hosts/$hostId/role") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("userId" to targetUserId, "role" to role))
        }

    private val memberPrivateFields = listOf("email", "phoneNumber", "receiveMail", "marketingAgree", "createdAt")

    private fun assertNoPrivateMemberFields(data: JsonNode) {
        (listOf(data.at("/masterUser")) + data.at("/hostUsers").toList()).forEach { member ->
            assertTrue(member.has("userId") && member.has("userName") && member.has("role"), "멤버 기본 정보 누락: $member")
            memberPrivateFields.forEach { assertFalse(member.has(it), "멤버 응답에 $it 노출: $member") }
        }
    }

    @Nested
    @DisplayName("H-1 호스트 상세")
    inner class HostDetail {

        @Test
        fun `비멤버와 초대 대기자는 호스트 상세를 볼 수 없다`() {
            val team = Team()
            val pending = newUser("초대대기")
            addPendingMember(team.hostId, pending, HostRole.GUEST)

            getHost(team.outsider, team.hostId).expectCode(400, "HOST_400_2")
            getHost(pending, team.hostId).expectCode(400, "HOST_400_6")
        }

        @Test
        fun `일반 멤버에게는 멤버 개인정보와 slackUrl 이 내려가지 않는다`() {
            val team = Team()
            val phoneMember = userWithPhone("전화있음")
            addPendingMember(team.hostId, phoneMember, HostRole.GUEST)
            setSlackUrl(team.hostId, "https://hooks.slack.com/services/v1-761")

            val result = getHost(team.guest, team.hostId).andExpect { status { isOk() } }
            val raw = result.raw()
            val data = objectMapper.readTree(raw).at("/data")

            assertTrue(data.at("/slackUrl").isNull, "일반 멤버에게 slackUrl 노출: $data")
            assertNoPrivateMemberFields(data)
            assertFalse(raw.contains("7761"), "응답에 전화번호 노출: $raw")
            assertFalse(raw.contains(phoneMember.profile!!.email!!), "응답에 이메일 노출: $raw")
            assertEquals(3, data.at("/hostUsers").size(), "마스터 외 멤버(매니저·일반·초대대기) 수")
        }

        @Test
        fun `매니저와 마스터에게는 slackUrl 이 내려간다`() {
            val team = Team()
            setSlackUrl(team.hostId, "https://hooks.slack.com/services/v1-761")

            listOf(team.manager, team.master).forEach {
                getHost(it, team.hostId).andExpect {
                    status { isOk() }
                    jsonPath("$.data.slackUrl") { value("https://hooks.slack.com/services/v1-761") }
                }
            }
        }
    }

    @Nested
    @DisplayName("H-2 초대 응답 / 초대 대상 검색")
    inner class InviteSearch {

        @Test
        fun `초대 응답에 초대한 사람의 전화번호가 없다`() {
            val team = Team()
            val invitee = userWithPhone("초대받는사람")

            val raw = invite(team.manager, team.hostId, invitee.profile!!.email!!, "GUEST").andExpect { status { isOk() } }.raw()
            assertFalse(raw.contains("7761"), "초대 응답에 전화번호 노출: $raw")
            assertNoPrivateMemberFields(objectMapper.readTree(raw).at("/data"))
        }

        @Test
        fun `초대 대상 검색은 매니저 이상만, 응답은 아이디·이름·프로필만`() {
            val team = Team()
            val target = userWithPhone("검색대상")
            val email = target.profile!!.email!!

            mockMvc.get("/api/v1/hosts/${team.hostId}/invite/users") {
                with(auth(team.guest))
                param("email", email)
            }.expectCode(400, "HOST_400_1")

            val data = mockMvc.get("/api/v1/hosts/${team.hostId}/invite/users") {
                with(auth(team.manager))
                param("email", email)
            }.andExpect { status { isOk() } }.data()
            assertEquals(target.id, data.at("/userId").asLong())
            assertEquals("검색대상", data.at("/userName").asText())
            listOf("email", "phoneNumber").forEach { assertFalse(data.has(it), "검색 응답에 $it 노출: $data") }
        }
    }

    @Nested
    @DisplayName("M-6 v1 역할 규칙")
    inner class RoleRule {

        @Test
        fun `매니저는 GUEST 로만 초대할 수 있다`() {
            val team = Team()
            invite(team.manager, team.hostId, newUser().profile!!.email!!, "MANAGER").expectCode(400, "HOST_400_11")
            invite(team.manager, team.hostId, newUser().profile!!.email!!, "GUEST").andExpect { status { isOk() } }
        }

        @Test
        fun `MASTER 로는 초대할 수 없고, 마스터는 MANAGER 로 초대할 수 있다`() {
            val team = Team()
            invite(team.master, team.hostId, newUser().profile!!.email!!, "MASTER").expectCode(400, "HOST_400_10")
            val invitee = newUser()
            invite(team.master, team.hostId, invitee.profile!!.email!!, "MANAGER").andExpect { status { isOk() } }
            assertEquals(HostRole.MANAGER, hostRepository.findById(team.hostId).get().getHostUserByUserId(invitee.id!!).role)
        }

        @Test
        fun `역할 변경은 마스터만, MASTER 로는 바꿀 수 없다`() {
            val team = Team()
            changeRole(team.manager, team.hostId, team.guest.id!!, "MANAGER").expectCode(400, "HOST_400_4")
            changeRole(team.master, team.hostId, team.guest.id!!, "MASTER").expectCode(400, "HOST_400_10")

            changeRole(team.master, team.hostId, team.guest.id!!, "MANAGER").andExpect { status { isOk() } }
            val host = hostRepository.findById(team.hostId).get()
            assertEquals(HostRole.MANAGER, host.getHostUserByUserId(team.guest.id!!).role)
            assertEquals(team.master.id, host.masterUserId)
        }
    }

    @Nested
    @DisplayName("H-3 v1 티켓·옵션 쓰기는 매니저 이상")
    inner class TicketWrite {

        private val v1Ticket = mapOf(
            "payType" to "무료티켓", "name" to "v1티켓", "description" to "설명", "price" to 0, "supplyCount" to 10,
            "approveType" to "선착순", "isQuantityPublic" to true, "purchaseLimit" to 2,
        )

        private val v1Option = mapOf("type" to "Y/N", "name" to "v1옵션", "description" to "설명", "additionalPrice" to 0)

        private fun post(requester: User, url: String, body: Any): ResultActionsDsl =
            mockMvc.post(url) {
                with(auth(requester))
                contentType = MediaType.APPLICATION_JSON
                content = json(body)
            }

        private fun patch(requester: User, url: String, body: Any? = null): ResultActionsDsl =
            mockMvc.patch(url) {
                with(auth(requester))
                contentType = MediaType.APPLICATION_JSON
                body?.let { content = json(it) }
            }

        @Test
        fun `일반 멤버는 티켓·옵션을 생성·삭제·적용·해제할 수 없고 매니저는 할 수 있다`() {
            val team = Team()
            val base = "/api/v1/events/${team.eventId}"

            post(team.guest, "$base/ticketItems", v1Ticket).expectCode(400, "HOST_400_1")
            post(team.guest, "$base/ticketOptions", v1Option).expectCode(400, "HOST_400_1")

            val ticketItemId = post(team.manager, "$base/ticketItems", v1Ticket).andExpect { status { isOk() } }.data().at("/ticketItemId").asLong()
            val optionGroupId = post(team.manager, "$base/ticketOptions", v1Option).andExpect { status { isOk() } }.data().at("/optionGroupId").asLong()
            val apply = mapOf("optionGroupId" to optionGroupId)

            patch(team.guest, "$base/ticketItems/$ticketItemId/option", apply).expectCode(400, "HOST_400_1")
            patch(team.manager, "$base/ticketItems/$ticketItemId/option", apply).andExpect { status { isOk() } }
            patch(team.guest, "$base/ticketItems/$ticketItemId/option/cancel", apply).expectCode(400, "HOST_400_1")
            patch(team.manager, "$base/ticketItems/$ticketItemId/option/cancel", apply).andExpect { status { isOk() } }

            patch(team.guest, "$base/ticketOptions/$optionGroupId").expectCode(400, "HOST_400_1")
            patch(team.guest, "$base/ticketItems/$ticketItemId").expectCode(400, "HOST_400_1")
            patch(team.manager, "$base/ticketOptions/$optionGroupId").andExpect { status { isOk() } }
            patch(team.manager, "$base/ticketItems/$ticketItemId").andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("M-1 v1 티켓 조회")
    inner class TicketRead {

        @Test
        fun `공개 티켓 목록은 준비중 공연이면 404, 공개 공연이면 계좌 없이 내려간다`() {
            val team = Team()
            createTicket(team.master, team.eventId)

            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems").expectCode(404, "Event_404_1")

            setEventStatus(team.eventId, EventStatus.OPEN)
            val items = mockMvc.get("/api/v1/events/${team.eventId}/ticketItems").andExpect { status { isOk() } }.data().at("/ticketItems")
            assertEquals(1, items.size())
            assertTrue(items[0].at("/accountInfo").isNull, "공개 티켓 목록에 계좌 노출: ${items[0]}")

            // 호스트 관리용 목록에는 계좌가 그대로 있다
            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems/admin") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.ticketItems[0].accountInfo.accountNumber") { value("110-123-456789") }
            }
        }

        @Test
        fun `옵션 적용 현황과 이벤트 옵션 목록은 호스트 멤버만`() {
            val team = Team()
            createTicket(team.master, team.eventId)
            setEventStatus(team.eventId, EventStatus.OPEN)

            listOf("ticketItems/appliedOptionGroups", "ticketOptions").forEach { path ->
                mockMvc.get("/api/v1/events/${team.eventId}/$path") { with(auth(team.outsider)) }.expectCode(400, "HOST_400_2")
                mockMvc.get("/api/v1/events/${team.eventId}/$path") { with(auth(team.guest)) }.andExpect { status { isOk() } }
            }
        }

        @Test
        fun `구매 플로우의 티켓 옵션 조회는 일반 사용자도 그대로 쓸 수 있다`() {
            val team = Team()
            val ticketItemId = createTicket(team.master, team.eventId)
            setEventStatus(team.eventId, EventStatus.OPEN)

            mockMvc.get("/api/v1/events/${team.eventId}/ticketItems/$ticketItemId/options") { with(auth(team.outsider)) }
                .andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("X-1 공연 상세 / X-3 호스트 프로필 이미지")
    inner class ContentAndImage {

        private fun patchDetail(requester: User, eventId: Long, posterImageKey: String, body: String): ResultActionsDsl =
            mockMvc.patch("/api/v1/events/$eventId/details") {
                with(auth(requester))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("posterImageKey" to posterImageKey, "content" to body))
            }

        private fun patchProfile(requester: User, hostId: Long, profileImageKey: String): ResultActionsDsl =
            mockMvc.patch("/api/v1/hosts/$hostId/profile") {
                with(auth(requester))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf("profileImageKey" to profileImageKey, "introduce" to "소개", "contactNumber" to "010-1111-2222", "contactEmail" to "h@gosrock.band"),
                )
            }

        @Test
        fun `공연 상세 본문의 위험 태그·속성은 제거되고 에디터 서식은 남는다`() {
            val team = Team()
            val poster = V2ImageKeys.issued(presignedUrlService.eventImageKeyPrefix(team.eventId))
            val content = "<h2>공지</h2><p>안녕<script>alert(1)</script><img src=\"https://cdn.example.com/a.png\" onerror=\"alert(2)\">" +
                "<a href=\"javascript:alert(3)\">링크</a><span style=\"color: #ff0000\">빨강</span>" +
                "<span style=\"position:fixed;top:0\">덮기</span><del>취소</del></p><hr><iframe src=\"https://evil.example.com\"></iframe>"

            patchDetail(team.manager, team.eventId, poster, content).andExpect { status { isOk() } }

            val saved = eventRepository.findById(team.eventId).get().eventDetail!!.content!!
            listOf("<script", "alert(1)", "onerror", "javascript:", "position:fixed", "<iframe").forEach {
                assertFalse(saved.contains(it), "sanitize 누락($it): $saved")
            }
            listOf("<h2>공지</h2>", "<span style=\"color: #ff0000\">빨강</span>", "<del>취소</del>", "<hr>", "src=\"https://cdn.example.com/a.png\"", "덮기").forEach {
                assertTrue(saved.contains(it), "서식 손실($it): $saved")
            }
        }

        @Test
        fun `공연 포스터 key 는 이 공연에 발급한 key 이거나 지금 저장된 key 여야 한다`() {
            val team = Team()
            val prefix = presignedUrlService.eventImageKeyPrefix(team.eventId)
            val other = presignedUrlService.eventImageKeyPrefix(team.eventId + 100_000)

            V2ImageKeys.rejected(prefix, other).forEach {
                patchDetail(team.manager, team.eventId, it, "<p>본문</p>").expectCode(400, "Event_400_22")
            }
            val poster = V2ImageKeys.issued(prefix)
            patchDetail(team.manager, team.eventId, poster, "<p>본문</p>").andExpect { status { isOk() } }
            patchDetail(team.manager, team.eventId, poster, "<p>본문 수정</p>").andExpect { status { isOk() } }
        }

        @Test
        fun `호스트 프로필 key 는 이 호스트에 발급한 key 이거나 지금 저장된 key 여야 한다`() {
            val team = Team()
            val prefix = presignedUrlService.hostImageKeyPrefix(team.hostId)
            val other = presignedUrlService.hostImageKeyPrefix(team.hostId + 100_000)

            V2ImageKeys.rejected(prefix, other).forEach {
                patchProfile(team.manager, team.hostId, it).expectCode(400, "HOST_400_17")
            }
            val key = V2ImageKeys.issued(prefix)
            patchProfile(team.manager, team.hostId, key).andExpect { status { isOk() } }
            patchProfile(team.manager, team.hostId, key).andExpect { status { isOk() } }
        }
    }
}
