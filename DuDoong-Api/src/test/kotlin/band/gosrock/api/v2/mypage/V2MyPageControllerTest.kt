package band.gosrock.api.v2.mypage

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.V2OperationTestSupport
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventDetail
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicket
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketItemInfoVo
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketUserInfoVo
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManagerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * v2 마이페이지 API 통합 테스트 (#729): M-1 ~ M-5.
 * 컨텍스트(H2)를 다른 테스트와 공유하지만 모든 조회가 요청자 본인 기준이라 테스트마다 새 유저로 격리된다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 마이페이지 API")
class V2MyPageControllerTest : V2OperationTestSupport() {

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private val now: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

    // ===== fixtures =====

    private fun me(requester: User?): ResultActionsDsl = mockMvc.get("/api/v2/me") { requester?.let { with(auth(it)) } }

    private fun patchMe(requester: User?, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.patch("/api/v2/me") {
            requester?.let { with(auth(it)) }
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    private fun following(requester: User, params: Map<String, String> = emptyMap()): JsonNode =
        v2Get(requester, "/me/following-hosts", params).andExpect { status { isOk() } }.data()

    private fun archive(requester: User, params: Map<String, String> = emptyMap()): JsonNode =
        v2Get(requester, "/me/archive", params).andExpect { status { isOk() } }.data()

    private fun follow(user: User, hostId: Long) {
        mockMvc.put("/api/v2/hosts/$hostId/follow") { with(auth(user)) }.andExpect { status { isOk() } }
    }

    private fun unfollow(user: User, hostId: Long) {
        mockMvc.delete("/api/v2/hosts/$hostId/follow") { with(auth(user)) }.andExpect { status { isOk() } }
    }

    private fun addMember(master: User, hostId: Long, member: User, role: String = "GUEST") {
        mockMvc.post("/api/v2/hosts/$hostId/members") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("members" to listOf(mapOf("email" to member.profile!!.email, "role" to role))))
        }.andExpect { status { isOk() } }
    }

    /** 공연을 저장소로 바로 만든다 (상태·일정·포스터 지정). 삭제 공연은 @Where 로 조회에서 빠진다 */
    private fun event(hostId: Long, status: EventStatus, startAt: LocalDateTime?, runTime: Long? = 120, name: String = "공연", poster: String? = null): Long {
        val event = Event(hostId = hostId, name = name, startAt = startAt, runTime = runTime)
        if (startAt == null) ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = name, startAt = null, runTime = runTime))
        ReflectionTestUtils.setField(event, "status", status)
        poster?.let { ReflectionTestUtils.setField(event, "eventDetail", EventDetail(posterImageKey = it, content = null)) }
        return eventRepository.save(event).id!!
    }

    private fun setSchedule(eventId: Long, status: EventStatus, startAt: LocalDateTime, runTime: Long = 120) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = event.getEventName(), startAt = startAt, runTime = runTime))
        ReflectionTestUtils.setField(event, "status", status)
        eventRepository.save(event)
    }

    /** 발급 티켓을 저장소로 바로 만든다. 주문 행은 없다(orderUuid 는 임의 값) */
    private fun ticket(owner: User, eventId: Long, status: IssuedTicketStatus, orderUuid: String = "fake-${UUID.randomUUID()}"): IssuedTicket =
        issuedTicketRepository.save(
            IssuedTicket(
                eventId = eventId,
                userInfo = IssuedTicketUserInfoVo.from(owner),
                orderUuid = orderUuid,
                itemInfo = IssuedTicketItemInfoVo(ticketName = "일반", price = Money.wons(1000)),
                issuedTicketStatus = status,
            ),
        )

    private fun JsonNode.ids(field: String): List<Long> = at("/content").map { it.at("/$field").asLong() }

    private fun countQueries(block: () -> Unit): Long {
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        try {
            statistics.clear()
            block()
            return statistics.prepareStatementCount
        } finally {
            statistics.isStatisticsEnabled = false
        }
    }

    // ===== 권한 =====

    @Nested
    @DisplayName("권한")
    inner class Auth {

        @Test
        fun `비로그인은 모든 마이페이지 API 가 401`() {
            me(null).andExpect { status { isUnauthorized() } }
            patchMe(null, mapOf("name" to "두둥")).andExpect { status { isUnauthorized() } }
            v2Post(null, "/me/images", mapOf("extension" to "PNG")).andExpect { status { isUnauthorized() } }
            v2Get(null, "/me/following-hosts").andExpect { status { isUnauthorized() } }
            v2Get(null, "/me/archive").andExpect { status { isUnauthorized() } }
        }
    }

    // ===== M-1 =====

    @Nested
    @DisplayName("M-1 내 프로필")
    inner class Me {

        @Test
        fun `닉네임·이메일·프로필 이미지 — 이메일은 v1 GET users me 와 같은 값`() {
            val user = newUser("두둥이")
            val data = me(user).andExpect { status { isOk() } }.data()
            assertEquals(user.id, data.at("/userId").asLong())
            assertEquals("두둥이", data.at("/name").asText())
            val v1 = mockMvc.get("/api/v1/users/me") { with(auth(user)) }.andExpect { status { isOk() } }.data()
            assertEquals(v1.at("/email").asText(), data.at("/email").asText())
            assertEquals(user.profile!!.email, data.at("/email").asText())
            assertTrue(data.at("/profileImageUrl").isNull)
            assertEquals(0, data.at("/hosts").size())
            assertEquals(0, data.at("/hostCount").asLong())
        }

        @Test
        fun `카카오 가입 이미지는 카카오 url 그대로 (v1 과 같음)`() {
            val user = newUser()
            user.profile!!.profileImage = band.gosrock.domain.common.vo.ImageVo.valueOf("http://k.kakaocdn.net/img.jpg")
            userRepository.save(user)
            val data = me(user).data()
            assertEquals("http://k.kakaocdn.net/img.jpg", data.at("/profileImageUrl").asText())
            assertEquals(mockMvc.get("/api/v1/users/me") { with(auth(user)) }.data().at("/profileImage").asText(), data.at("/profileImageUrl").asText())
        }

        @Test
        fun `소속 호스트 — 합류 최신순(호스트 id 순 아님), 내 역할, 비활성·남의 호스트 제외`() {
            val user = newUser("나")
            val other = newUser("남")
            // 남이 먼저 만든 호스트(id 작음)에 내가 나중에 합류한다
            val oldHost = createHost(other, "옛호스트")
            val myFirst = createHost(user, "내첫호스트")
            val mySecond = createHost(user, "내둘째")
            addMember(other, oldHost, user, role = "MANAGER")
            createHost(other, "남의호스트")
            // v1 초대만 받고 수락하지 않은 호스트(비활성 멤버)는 제외
            val invitedHost = createHost(other, "초대만")
            mockMvc.post("/api/v1/hosts/$invitedHost/invite") {
                with(auth(other))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("email" to user.profile!!.email, "role" to "GUEST"))
            }.andExpect { status { isOk() } }

            val data = me(user).data()
            assertEquals(listOf(oldHost, mySecond, myFirst), data.at("/hosts").map { it.at("/hostId").asLong() })
            assertEquals(listOf("MANAGER", "MASTER", "MASTER"), data.at("/hosts").map { it.at("/myRole").asText() })
            assertEquals("옛호스트", data.at("/hosts/0/name").asText())
            assertEquals(3, data.at("/hostCount").asLong())
        }

        @Test
        fun `소속 호스트는 최대 10개, hostCount 는 전체 수`() {
            val user = newUser()
            val hostIds = (1..11).map { createHost(user, "호스트$it") }
            val data = me(user).data()
            assertEquals(10, data.at("/hosts").size())
            assertEquals(hostIds.reversed().take(10), data.at("/hosts").map { it.at("/hostId").asLong() })
            assertEquals(11, data.at("/hostCount").asLong())
        }
    }

    // ===== M-2 / M-3 =====

    @Nested
    @DisplayName("M-2 프로필 수정 / M-3 이미지 업로드")
    inner class Update {

        @Test
        fun `닉네임만 수정 — null 인 이미지는 그대로, 응답은 M-1 과 같은 모양`() {
            val user = newUser("원래")
            val key = "${presignedUrlService.userImageKeyPrefix(user.id!!)}a.png"
            patchMe(user, mapOf("profileImageKey" to key)).andExpect { status { isOk() } }
            val data = patchMe(user, mapOf("name" to "새이름")).andExpect { status { isOk() } }.data()
            assertEquals("새이름", data.at("/name").asText())
            assertTrue(data.at("/profileImageUrl").asText().endsWith("/user/${user.id}/a.png"))
            assertEquals("새이름", userRepository.findById(user.id!!).get().profile!!.name)
            assertEquals(me(user).data(), data)
        }

        @Test
        fun `빈 body 는 아무것도 바꾸지 않는다`() {
            val user = newUser("그대로")
            patchMe(user, emptyMap()).andExpect { status { isOk() } }
            assertEquals("그대로", userRepository.findById(user.id!!).get().profile!!.name)
        }

        @Test
        fun `닉네임 — v1 이름 변경 API 와 같은 입력에서 같은 결과 (성공 또는 400)`() {
            val inputs = listOf("두둥", "ab", "abcdefg", " a ", "a", "abcdefgh", "", " ", "       ", "\t\t", "가나다라마바사아", "😀😀😀", "😀😀😀😀")
            inputs.forEach { name ->
                val v1User = newUser("v1")
                val v2User = newUser("v2")
                val v1 = mockMvc.patch("/api/v1/users/me/name") {
                    with(auth(v1User))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("name" to name))
                }.andReturn().response.status
                val v2 = patchMe(v2User, mapOf("name" to name)).andReturn().response.status
                assertEquals(v1, v2, "입력 \"$name\"")
                if (v1 == 200) {
                    assertEquals(userRepository.findById(v1User.id!!).get().profile!!.name, userRepository.findById(v2User.id!!).get().profile!!.name)
                } else {
                    assertEquals(400, v2)
                    assertEquals("v2", userRepository.findById(v2User.id!!).get().profile!!.name)
                }
            }
        }

        @Test
        fun `닉네임 — 요청 검증은 통과하지만 유니코드 공백만인 이름은 v2 는 400 USER_400_4 (v1 은 500)`() {
            val user = newUser("원래")
            patchMe(user, mapOf("name" to "　　")).andExpect { status { isBadRequest() } }.also { assertEquals("USER_400_4", it.code()) }
            assertEquals("원래", userRepository.findById(user.id!!).get().profile!!.name)
            val v1 = mockMvc.patch("/api/v1/users/me/name") {
                with(auth(newUser()))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "　　"))
            }.andReturn().response.status
            assertEquals(500, v1)
        }

        @Test
        fun `프로필 이미지 — 본인 경로 key 저장, 빈 문자열이면 기본 이미지(null)`() {
            val user = newUser()
            val key = "${presignedUrlService.userImageKeyPrefix(user.id!!)}${UUID.randomUUID()}.png"
            val saved = patchMe(user, mapOf("profileImageKey" to key)).andExpect { status { isOk() } }.data()
            assertTrue(saved.at("/profileImageUrl").asText().endsWith(key))
            assertEquals(key, userRepository.findById(user.id!!).get().profile!!.profileImage!!.imageKey)

            val cleared = patchMe(user, mapOf("profileImageKey" to "")).andExpect { status { isOk() } }.data()
            assertTrue(cleared.at("/profileImageUrl").isNull)
            assertNull(userRepository.findById(user.id!!).get().profile!!.profileImage?.imageKey)
        }

        @Test
        fun `프로필 이미지 — 남의 경로·외부 url·경로 이탈 key 는 400 USER_400_5, 저장 안 됨`() {
            val user = newUser()
            val other = newUser()
            listOf(
                "${presignedUrlService.userImageKeyPrefix(other.id!!)}a.png",
                "https://evil.example.com/a.png",
                "${presignedUrlService.userImageKeyPrefix(user.id!!)}../${other.id}/a.png",
                "${presignedUrlService.hostImageKeyPrefix(1)}a.png",
            ).forEach { key ->
                patchMe(user, mapOf("profileImageKey" to key)).andExpect { status { isBadRequest() } }.also { assertEquals("USER_400_5", it.code(), key) }
            }
            assertNull(userRepository.findById(user.id!!).get().profile!!.profileImage?.imageKey)
        }

        @Test
        fun `M-3 업로드 url — 본인 경로 key, 허용 확장자만`() {
            val user = newUser()
            val data = v2Post(user, "/me/images", mapOf("extension" to "JPG")).andExpect { status { isOk() } }.data()
            assertTrue(data.at("/key").asText().startsWith(presignedUrlService.userImageKeyPrefix(user.id!!)))
            assertTrue(data.at("/key").asText().endsWith(".jpeg"))
            assertTrue(data.at("/presignedUrl").asText().isNotEmpty())
            assertTrue(data.at("/url").asText().endsWith(data.at("/key").asText()))
            // 발급받은 key 는 M-2 로 저장할 수 있다
            patchMe(user, mapOf("profileImageKey" to data.at("/key").asText())).andExpect { status { isOk() } }

            v2Post(user, "/me/images", mapOf("extension" to "GIF")).andExpect { status { isBadRequest() } }
            v2Post(user, "/me/images", mapOf("extension" to null)).andExpect { status { isBadRequest() } }
        }
    }

    // ===== M-4 =====

    @Nested
    @DisplayName("M-4 관심 호스트")
    inner class Following {

        private fun repOf(data: JsonNode, hostId: Long): JsonNode =
            data.at("/content").first { it.at("/hostId").asLong() == hostId }.at("/representativeEvent")

        @Test
        fun `대표 공연 분기 — 진행 중, 예정, 끝남만, 공연 없음, 준비중만`() {
            val user = newUser()
            val owner = newUser()
            val ongoingHost = createHost(owner, "진행호스트")
            val ongoing = event(ongoingHost, EventStatus.OPEN, now.minusHours(1), name = "진행중")
            event(ongoingHost, EventStatus.OPEN, now.plusDays(1), name = "내일")
            event(ongoingHost, EventStatus.CLOSED, now.minusDays(1))

            val upcomingHost = createHost(owner, "예정호스트")
            event(upcomingHost, EventStatus.OPEN, now.plusDays(10))
            val soon = event(upcomingHost, EventStatus.OPEN, now.plusDays(3), name = "곧", poster = "event/1/p.png")
            event(upcomingHost, EventStatus.PREPARING, now.plusDays(1))
            event(upcomingHost, EventStatus.CALCULATING, now.minusHours(5))

            val endedHost = createHost(owner, "끝난호스트")
            event(endedHost, EventStatus.CLOSED, now.minusDays(30))
            val lastEnded = event(endedHost, EventStatus.CALCULATING, now.minusDays(2), name = "최근끝")
            event(endedHost, EventStatus.DELETED, now.minusDays(1))
            event(endedHost, EventStatus.PREPARING, now.plusDays(1))

            val emptyHost = createHost(owner, "빈호스트")
            val preparingOnlyHost = createHost(owner, "준비호스트")
            event(preparingOnlyHost, EventStatus.PREPARING, now.plusDays(1))
            event(preparingOnlyHost, EventStatus.DELETED, now.plusDays(1))

            listOf(ongoingHost, upcomingHost, endedHost, emptyHost, preparingOnlyHost).forEach { follow(user, it) }
            val data = following(user)

            repOf(data, ongoingHost).let {
                assertEquals(ongoing, it.at("/eventId").asLong())
                assertEquals("ONGOING", it.at("/displayStatus").asText())
                assertTrue(it.at("/dDay").isNull)
            }
            repOf(data, upcomingHost).let {
                assertEquals(soon, it.at("/eventId").asLong())
                assertEquals("곧", it.at("/name").asText())
                assertEquals("UPCOMING", it.at("/displayStatus").asText())
                assertEquals(3, it.at("/dDay").asLong())
                assertTrue(it.at("/posterImageUrl").asText().endsWith("event/1/p.png"))
            }
            repOf(data, endedHost).let {
                assertEquals(lastEnded, it.at("/eventId").asLong())
                assertEquals("PAST", it.at("/displayStatus").asText())
                assertTrue(it.at("/dDay").isNull)
            }
            assertTrue(repOf(data, emptyHost).isNull)
            assertTrue(repOf(data, preparingOnlyHost).isNull)
            assertEquals("빈호스트", data.at("/content").first { it.at("/hostId").asLong() == emptyHost }.at("/name").asText())
        }

        @Test
        fun `정렬은 최근 팔로우 순, 페이지 (size·hasNext·totalElements)`() {
            val user = newUser()
            val owner = newUser()
            val hosts = (1..5).map { createHost(owner, "팔로우$it") }
            // 팔로우 순서: 3, 1, 5, 2, 4 → 목록은 역순
            listOf(hosts[2], hosts[0], hosts[4], hosts[1], hosts[3]).forEach { follow(user, it) }
            val expected = listOf(hosts[3], hosts[1], hosts[4], hosts[0], hosts[2])

            val first = following(user, mapOf("size" to "2"))
            assertEquals(expected.take(2), first.ids("hostId"))
            assertEquals(5, first.at("/totalElements").asLong())
            assertEquals(3, first.at("/totalPages").asInt())
            assertTrue(first.at("/hasNext").asBoolean())
            assertEquals(expected.subList(2, 4), following(user, mapOf("size" to "2", "page" to "1")).ids("hostId"))
            val last = following(user, mapOf("size" to "2", "page" to "2"))
            assertEquals(expected.subList(4, 5), last.ids("hostId"))
            assertFalse(last.at("/hasNext").asBoolean())
        }

        @Test
        fun `목록에서 언팔로우(H-13 재사용)하면 빠지고, 다시 팔로우하면 맨 앞`() {
            val user = newUser()
            val owner = newUser()
            val a = createHost(owner, "A")
            val b = createHost(owner, "B")
            follow(user, a)
            follow(user, b)
            assertEquals(listOf(b, a), following(user).ids("hostId"))
            unfollow(user, b)
            assertEquals(listOf(a), following(user).ids("hostId"))
            follow(user, b)
            assertEquals(listOf(b, a), following(user).ids("hostId"))
        }

        @Test
        fun `남의 팔로우는 보이지 않는다`() {
            val user = newUser()
            val other = newUser()
            val owner = newUser()
            val mine = createHost(owner, "내것")
            val theirs = createHost(owner, "남의것")
            follow(user, mine)
            follow(other, theirs)
            assertEquals(listOf(mine), following(user).ids("hostId"))
            assertEquals(listOf(theirs), following(other).ids("hostId"))
            assertEquals(0, following(newUser()).at("/content").size())
        }

        @Test
        fun `전체 상태 필터 — ACTIVE 는 진행 중·예정 공연 있음, ENDED 는 끝난 공연만, 공연 없는 호스트는 ALL 에만`() {
            val user = newUser()
            val owner = newUser()
            val ongoingHost = createHost(owner, "진행").also { event(it, EventStatus.OPEN, now.minusHours(1)) }
            val upcomingHost = createHost(owner, "예정").also {
                event(it, EventStatus.OPEN, now.plusDays(1))
                event(it, EventStatus.CLOSED, now.minusDays(1))
            }
            // 종료 시각이 지났지만 종료 배치 전인 OPEN 은 끝난 공연
            val endedHost = createHost(owner, "끝남").also { event(it, EventStatus.OPEN, now.minusHours(5)) }
            val emptyHost = createHost(owner, "없음").also { event(it, EventStatus.PREPARING, now.plusDays(1)) }
            listOf(ongoingHost, upcomingHost, endedHost, emptyHost).forEach { follow(user, it) }

            assertEquals(listOf(emptyHost, endedHost, upcomingHost, ongoingHost), following(user).ids("hostId"))
            assertEquals(listOf(upcomingHost, ongoingHost), following(user, mapOf("status" to "ACTIVE")).ids("hostId"))
            assertEquals(listOf(endedHost), following(user, mapOf("status" to "ENDED")).ids("hostId"))
            // 필터 결과의 대표 공연 표시 상태가 필터와 맞다
            following(user, mapOf("status" to "ACTIVE")).at("/content").forEach {
                assertTrue(it.at("/representativeEvent/displayStatus").asText() in listOf("ONGOING", "UPCOMING"))
            }
            assertEquals(2, following(user, mapOf("status" to "ACTIVE")).at("/totalElements").asLong())
            v2Get(user, "/me/following-hosts", mapOf("status" to "NOPE")).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `N+1 없음 — 페이지 크기·호스트별 공연 수와 관계없이 쿼리 수가 같다`() {
            val user = newUser()
            val owner = newUser()
            val hosts = (1..4).map { createHost(owner, "N1-$it") }
            hosts.forEachIndexed { i, hostId ->
                repeat(i + 1) { n -> event(hostId, EventStatus.CLOSED, now.minusDays(n + 1L)) }
                event(hostId, EventStatus.OPEN, now.plusDays(i + 1L))
                follow(user, hostId)
            }
            // 목록 1 + 건수 1 + 공개 공연 1. 한 페이지에 다 들어오면 건수 쿼리를 생략한다
            val one = countQueries { following(user, mapOf("size" to "1")) }
            val three = countQueries { following(user, mapOf("size" to "3")) }
            val all = countQueries { following(user, mapOf("size" to "50")) }
            assertEquals(one, three, "size=1: $one, size=3: $three")
            assertEquals(3, three)
            assertEquals(2, all)
        }
    }

    // ===== M-5 =====

    @Nested
    @DisplayName("M-5 관람 공연 아카이빙")
    inner class Archive {

        @Test
        fun `입장한 지난 공연만, 공연 단위 중복 없음, 최근 공연 순 — 미입장·취소·진행 중·예정·준비중·삭제 제외`() {
            val user = newUser()
            val owner = newUser()
            val hostId = createHost(owner, "아카호스트")
            val closed = event(hostId, EventStatus.CLOSED, now.minusDays(10), name = "종료공연", poster = "event/9/p.png")
            val calculating = event(hostId, EventStatus.CALCULATING, now.minusDays(2))
            val endedOpen = event(hostId, EventStatus.OPEN, now.minusHours(5))
            val ongoing = event(hostId, EventStatus.OPEN, now.minusHours(1))
            val upcoming = event(hostId, EventStatus.OPEN, now.plusDays(1))
            val notEntered = event(hostId, EventStatus.CLOSED, now.minusDays(3))
            val canceled = event(hostId, EventStatus.CLOSED, now.minusDays(4))
            val deleted = event(hostId, EventStatus.DELETED, now.minusDays(5))
            val preparing = event(hostId, EventStatus.PREPARING, now.minusDays(6))

            // 같은 공연 3장 입장 + 1장 미입장
            repeat(3) { ticket(user, closed, IssuedTicketStatus.ENTRANCE_COMPLETED) }
            ticket(user, closed, IssuedTicketStatus.ENTRANCE_INCOMPLETE)
            listOf(calculating, endedOpen, ongoing, upcoming, deleted, preparing).forEach { ticket(user, it, IssuedTicketStatus.ENTRANCE_COMPLETED) }
            ticket(user, notEntered, IssuedTicketStatus.ENTRANCE_INCOMPLETE)
            ticket(user, canceled, IssuedTicketStatus.CANCELED)

            val data = archive(user)
            assertEquals(listOf(endedOpen, calculating, closed), data.at("/events").ids("eventId"))
            assertEquals(3, data.at("/events/totalElements").asLong())
            val item = data.at("/events/content/2")
            assertEquals("종료공연", item.at("/name").asText())
            assertTrue(item.at("/posterImageUrl").asText().endsWith("event/9/p.png"))
            assertEquals(hostId, item.at("/host/hostId").asLong())
            assertEquals("아카호스트", item.at("/host/name").asText())
            assertTrue(item.at("/startAt").asText().isNotEmpty())
            // 주문 행이 없는 티켓(임의 uuid)은 주문 상세로 갈 수 없다
            assertTrue(item.at("/orderUuid").isNull)
        }

        @Test
        fun `연도 필터와 연도 탭 — 공연 시작 연도 기준, 탭은 필터와 관계없이 전체`() {
            val user = newUser()
            val owner = newUser()
            val hostId = createHost(owner, "연도")
            val y2024 = event(hostId, EventStatus.CLOSED, LocalDateTime.of(2024, 12, 31, 23, 30))
            val y2025a = event(hostId, EventStatus.CLOSED, LocalDateTime.of(2025, 1, 1, 0, 0))
            val y2025b = event(hostId, EventStatus.CLOSED, LocalDateTime.of(2025, 6, 1, 18, 0))
            val y2023NotEntered = event(hostId, EventStatus.CLOSED, LocalDateTime.of(2023, 6, 1, 18, 0))
            listOf(y2024, y2025a, y2025b).forEach { ticket(user, it, IssuedTicketStatus.ENTRANCE_COMPLETED) }
            ticket(user, y2023NotEntered, IssuedTicketStatus.ENTRANCE_INCOMPLETE)

            val all = archive(user)
            assertEquals(listOf(2025, 2024), all.at("/years").map { it.asInt() })
            assertEquals(listOf(y2025b, y2025a, y2024), all.at("/events").ids("eventId"))
            val in2025 = archive(user, mapOf("year" to "2025"))
            assertEquals(listOf(y2025b, y2025a), in2025.at("/events").ids("eventId"))
            assertEquals(listOf(2025, 2024), in2025.at("/years").map { it.asInt() })
            assertEquals(listOf(y2024), archive(user, mapOf("year" to "2024")).at("/events").ids("eventId"))
            assertEquals(0, archive(user, mapOf("year" to "2023")).at("/events/content").size())
            v2Get(user, "/me/archive", mapOf("year" to "0")).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `페이지 — 공연 단위로 센다 (한 공연 여러 장이어도 1건)`() {
            val user = newUser()
            val owner = newUser()
            val hostId = createHost(owner, "페이지")
            val events = (1..5).map { event(hostId, EventStatus.CLOSED, now.minusDays(it.toLong())) }
            events.forEach { id -> repeat(2) { ticket(user, id, IssuedTicketStatus.ENTRANCE_COMPLETED) } }
            val first = archive(user, mapOf("size" to "2"))
            assertEquals(events.take(2), first.at("/events").ids("eventId"))
            assertEquals(5, first.at("/events/totalElements").asLong())
            assertTrue(first.at("/events/hasNext").asBoolean())
            val last = archive(user, mapOf("size" to "2", "page" to "2"))
            assertEquals(events.subList(4, 5), last.at("/events").ids("eventId"))
            assertFalse(last.at("/events/hasNext").asBoolean())
        }

        @Test
        fun `현재 소유자 기준 — 소유자가 바뀐 티켓은 받은 사람에게 보이고 원래 주인에게서는 빠진다`() {
            val sender = newUser("보낸사람")
            val receiver = newUser("받은사람")
            val owner = newUser()
            val hostId = createHost(owner, "선물")
            val giftedEvent = event(hostId, EventStatus.CLOSED, now.minusDays(3))
            val sharedEvent = event(hostId, EventStatus.CLOSED, now.minusDays(4))
            // 보낸 사람이 산 티켓 2장: 1장은 소유자 변경(선물 수락 후 입장), 1장은 본인 입장
            val gifted = ticket(sender, giftedEvent, IssuedTicketStatus.ENTRANCE_COMPLETED)
            gifted.userInfo = IssuedTicketUserInfoVo.from(receiver)
            issuedTicketRepository.save(gifted)
            ticket(sender, sharedEvent, IssuedTicketStatus.ENTRANCE_COMPLETED)
            val giftedShared = ticket(sender, sharedEvent, IssuedTicketStatus.ENTRANCE_COMPLETED)
            giftedShared.userInfo = IssuedTicketUserInfoVo.from(receiver)
            issuedTicketRepository.save(giftedShared)

            assertEquals(listOf(sharedEvent), archive(sender).at("/events").ids("eventId"))
            assertEquals(listOf(giftedEvent, sharedEvent), archive(receiver).at("/events").ids("eventId"))
            assertEquals(0, archive(newUser()).at("/events/content").size())
        }

        @Test
        fun `실제 주문 → 승인 → 입장 → 종료 — orderUuid 로 주문상세(O-3) 이동, 소유자가 바뀐 티켓만 입장한 사람은 orderUuid null`() {
            val shop = Shop("실주문")
            val buyer = newBuyer("구매자")
            val receiver = newBuyer("받은사람")
            val orderUuid = shop.approved(buyer, quantity = 2)
            val tickets = issuedTicketRepository.findAllByOrderUuid(orderUuid).sortedBy { it.id }
            checkIn(shop.team.master, shop.eventId, tickets[0].uuid!!).andExpect { status { isOk() } }
            // 두 번째 티켓은 받은 사람 소유로 바꾼 뒤 입장
            val second = issuedTicketRepository.findById(tickets[1].id!!).get()
            second.userInfo = IssuedTicketUserInfoVo.from(receiver)
            issuedTicketRepository.save(second)
            checkIn(shop.team.master, shop.eventId, second.uuid!!).andExpect { status { isOk() } }
            setSchedule(shop.eventId, EventStatus.CLOSED, now.minusDays(1))

            val mine = archive(buyer).at("/events/content")
            assertEquals(listOf(shop.eventId), mine.map { it.at("/eventId").asLong() })
            assertEquals(orderUuid, mine[0].at("/orderUuid").asText())
            myOrder(buyer, orderUuid).andExpect { status { isOk() } }

            val theirs = archive(receiver).at("/events/content")
            assertEquals(listOf(shop.eventId), theirs.map { it.at("/eventId").asLong() })
            assertTrue(theirs[0].at("/orderUuid").isNull)
        }

        @Test
        fun `N+1 없음 — 페이지 크기와 관계없이 쿼리 수가 같다`() {
            val user = newUser()
            val owner = newUser()
            val hosts = (1..3).map { createHost(owner, "아N1-$it") }
            hosts.forEachIndexed { i, hostId ->
                repeat(2) { n -> event(hostId, EventStatus.CLOSED, now.minusDays(i * 10L + n + 1)).also { ticket(user, it, IssuedTicketStatus.ENTRANCE_COMPLETED) } }
            }
            // 연도 1 + 목록 1 + 건수 1 + 호스트명 1 + 주문 uuid 1. 한 페이지에 다 들어오면 건수 쿼리를 생략한다
            val one = countQueries { archive(user, mapOf("size" to "1")) }
            val four = countQueries { archive(user, mapOf("size" to "4")) }
            val all = countQueries { archive(user, mapOf("size" to "50")) }
            assertEquals(one, four, "size=1: $one, size=4: $four")
            assertEquals(5, four)
            assertEquals(4, all)
        }
    }

    private fun myOrder(buyer: User, orderUuid: String): ResultActionsDsl = v2Get(buyer, "/me/orders/$orderUuid")
}
