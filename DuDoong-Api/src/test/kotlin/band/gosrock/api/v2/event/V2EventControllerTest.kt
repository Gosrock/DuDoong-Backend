package band.gosrock.api.v2.event

import band.gosrock.api.v2.support.V2ImageKeys
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.event.service.v2.V2EventDomainService
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagCategory
import band.gosrock.domain.domains.tag.domain.TagSeed
import band.gosrock.domain.domains.tag.repository.TagRepository
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketItemStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.ticket_item.repository.TicketItemRepository
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.OauthInfo
import band.gosrock.domain.domains.user.domain.OauthProvider
import band.gosrock.domain.domains.user.domain.Profile
import band.gosrock.domain.domains.user.domain.User
import band.gosrock.domain.domains.user.repository.UserRepository
import band.gosrock.infrastructure.config.s3.S3UploadPresignedUrlService
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManagerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

/**
 * v2 공연 준비 API 통합 테스트 (#705).
 * 컨텍스트(H2)가 테스트 간에 공유되므로 테스트마다 유저/호스트/공연을 새로 만든다. 태그는 V002 시드와 같은 값을 없으면 넣는다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공연 준비 API")
class V2EventControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var userRepository: UserRepository

    @Autowired private lateinit var hostRepository: HostRepository

    @Autowired private lateinit var eventRepository: EventRepository

    @Autowired private lateinit var v2EventDomainService: V2EventDomainService

    @Autowired private lateinit var tagRepository: TagRepository

    @Autowired private lateinit var ticketItemRepository: TicketItemRepository

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")

    private fun LocalDateTime.f(): String = format(fmt)

    /** 분 단위로 맞춘 기준 시각: 30일 뒤 18:00 */
    private val baseStart: LocalDateTime = LocalDate.now().plusDays(30).atTime(18, 0)

    @BeforeEach
    fun seedTags() {
        SEED_TAGS.forEach { (category, names) ->
            names.forEachIndexed { index, name ->
                if (!tagRepository.existsByCategoryAndName(category, name)) {
                    tagRepository.save(Tag(category = category, name = name, sortOrder = index))
                }
            }
        }
    }

    private fun tagId(category: TagCategory, name: String): Long =
        tagRepository.findAll().first { it.category == category && it.name == name }.id!!

    // ===== fixtures =====

    private fun newUser(name: String = "유저"): User =
        userRepository.save(
            User(
                profile = Profile(name = name, email = "v2event-${UUID.randomUUID()}@test.com", phoneNumber = null, profileImage = null),
                oauthInfo = OauthInfo(OauthProvider.KAKAO, "v2event-${UUID.randomUUID()}"),
            ),
        )

    private fun User.email(): String = profile!!.email!!

    private fun auth(u: User) = user(u.id.toString()).roles("USER")

    private fun json(body: Any?): String = objectMapper.writeValueAsString(body)

    private fun ResultActionsDsl.body(): JsonNode =
        objectMapper.readTree(andReturn().response.getContentAsString(Charsets.UTF_8))

    private fun createHost(master: User, name: String = "고스락"): Long =
        mockMvc.post("/api/v2/hosts") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("name" to name, "contacts" to listOf(mapOf("type" to "EMAIL", "value" to "h@gosrock.band"))))
        }.andExpect { status { isOk() } }.body().at("/data/hostId").asLong()

    /** 마스터 + 매니저 + 일반 + 외부인 */
    private inner class Team(hostName: String = "고스락") {
        val master = newUser("마스터")
        val manager = newUser("매니저")
        val guest = newUser("일반")
        val outsider = newUser("외부인")
        val hostId = createHost(master, hostName)

        init {
            mockMvc.post("/api/v2/hosts/$hostId/members") {
                with(auth(master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf(
                        "members" to listOf(
                            mapOf("email" to manager.email(), "role" to "MANAGER"),
                            mapOf("email" to guest.email(), "role" to "GUEST"),
                        ),
                    ),
                )
            }.andExpect { status { isOk() } }
        }
    }

    private fun createEventRequest(
        requester: User,
        hostId: Long,
        name: String = "정기공연",
        startAt: LocalDateTime = baseStart,
        endAt: LocalDateTime = baseStart.plusMinutes(200),
        hasTicket: Boolean? = true,
    ): ResultActionsDsl =
        mockMvc.post("/api/v2/events") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("hostId" to hostId, "name" to name, "startAt" to startAt.f(), "endAt" to endAt.f(), "hasTicket" to hasTicket))
        }

    private fun createEvent(requester: User, hostId: Long, name: String = "정기공연", hasTicket: Boolean = true): Long =
        createEventRequest(requester, hostId, name = name, hasTicket = hasTicket)
            .andExpect { status { isOk() } }.body().at("/data/eventId").asLong()

    private fun patchBasic(requester: User, eventId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.patch("/api/v2/events/$eventId/basic") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    private fun putSections(requester: User, eventId: Long, sections: List<Map<String, Any?>?>): ResultActionsDsl =
        mockMvc.put("/api/v2/events/$eventId/sections") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(sections)
        }

    private val placeBody = mapOf("name" to "롤링홀", "address" to "서울 마포구 어울마당로 35", "latitude" to 37.548369, "longitude" to 126.920036)

    private val defaultSections = listOf(
        mapOf("title" to "공연 소개", "content" to "<p>소개</p>", "sortOrder" to 0),
        mapOf("title" to "예매안내", "content" to "예매", "sortOrder" to 1),
        mapOf("title" to "세트리스트", "content" to "", "sortOrder" to 2),
        mapOf("title" to "유의사항", "content" to null, "sortOrder" to 3),
    )

    private fun posterKey(eventId: Long) = V2ImageKeys.issued(presignedUrlService.eventImageKeyPrefix(eventId))

    /** 기본 정보(포스터·장소·문의처) + 섹션까지 채운다 */
    private fun fillBasicAndDetail(requester: User, eventId: Long) {
        patchBasic(
            requester,
            eventId,
            mapOf("posterImageKey" to posterKey(eventId), "place" to placeBody, "contacts" to listOf(mapOf("type" to "INSTAGRAM", "value" to "@gosrock"))),
        ).andExpect { status { isOk() } }
        putSections(requester, eventId, defaultSections).andExpect { status { isOk() } }
    }

    private fun saveTicket(eventId: Long, status: TicketItemStatus = TicketItemStatus.VALID): TicketItem {
        val item = TicketItem(
            payType = TicketPayType.FREE_TICKET,
            name = "무료",
            description = "무료",
            price = Money.ZERO,
            quantity = 10L,
            supplyCount = 10L,
            purchaseLimit = 1L,
            type = TicketType.FIRST_COME_FIRST_SERVED,
            isQuantityPublic = true,
            isSellable = true,
            eventId = eventId,
        )
        ReflectionTestUtils.setField(item, "ticketItemStatus", status)
        return ticketItemRepository.save(item)
    }

    private fun setStatus(eventId: Long, status: EventStatus) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "status", status)
        eventRepository.save(event)
    }

    private fun openV2(requester: User, eventId: Long): ResultActionsDsl =
        mockMvc.post("/api/v2/events/$eventId/open") { with(auth(requester)) }

    private fun checklist(requester: User, eventId: Long): JsonNode =
        mockMvc.get("/api/v2/events/$eventId/checklist") { with(auth(requester)) }
            .andExpect { status { isOk() } }.body().at("/data")

    // ===== E-2 =====

    @Nested
    @DisplayName("E-2 간편 생성")
    inner class Create {

        @Test
        fun `매니저가 만들면 준비중이고 runTime 은 종료-시작 분, end_at 도 저장된다`() {
            val team = Team()
            val eventId = createEvent(team.manager, team.hostId)

            val event = eventRepository.findById(eventId).get()
            assertEquals(EventStatus.PREPARING, event.status)
            assertEquals(200L, event.eventBasic!!.runTime)
            assertEquals(baseStart.plusMinutes(200), event.storedEndAt)
            assertTrue(event.hasTicket)

            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.manager)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("PREPARING") }
                jsonPath("$.data.displayStatus") { value("PREPARING") }
                jsonPath("$.data.startAt") { value(baseStart.f()) }
                jsonPath("$.data.endAt") { value(baseStart.plusMinutes(200).f()) }
                jsonPath("$.data.runTime") { value(200) }
                jsonPath("$.data.hasTicket") { value(true) }
                jsonPath("$.data.myRole") { value("MANAGER") }
            }
        }

        @Test
        fun `일반 멤버, 비멤버는 403, 비로그인은 401, 없는 호스트는 404`() {
            val team = Team()
            createEventRequest(team.guest, team.hostId).andExpect { status { isForbidden() } }
            createEventRequest(team.outsider, team.hostId).andExpect { status { isForbidden() } }
            createEventRequest(team.master, 987654321L).andExpect { status { isNotFound() } }
            mockMvc.post("/api/v2/events") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("hostId" to team.hostId, "name" to "x", "startAt" to baseStart.f(), "endAt" to baseStart.plusHours(1).f(), "hasTicket" to true))
            }.andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `종료가 시작보다 빠르거나 같으면 400, 이름 26자 400, 지난 시작 400, hasTicket 누락 400`() {
            val team = Team()
            createEventRequest(team.master, team.hostId, endAt = baseStart).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_2") }
            }
            createEventRequest(team.master, team.hostId, endAt = baseStart.minusMinutes(10))
                .andExpect { jsonPath("$.code") { value("Event_400_2") } }
            createEventRequest(team.master, team.hostId, name = "가".repeat(26)).andExpect { status { isBadRequest() } }
            createEventRequest(team.master, team.hostId, name = "가".repeat(25)).andExpect { status { isOk() } }
            createEventRequest(team.master, team.hostId, name = "   ").andExpect { status { isBadRequest() } }
            createEventRequest(team.master, team.hostId, startAt = LocalDateTime.now().minusDays(1), endAt = LocalDateTime.now())
                .andExpect { status { isBadRequest() } }
            createEventRequest(team.master, team.hostId, hasTicket = null).andExpect { status { isBadRequest() } }
        }
    }

    // ===== E-1 =====

    @Nested
    @DisplayName("E-1 내 공연 목록")
    inner class MyEvents {

        @Test
        fun `활성 멤버인 모든 호스트의 공연, 삭제 제외, 최신 생성 순, 호스트명-장소-dDay`() {
            val teamA = Team("호스트A")
            val other = newUser("다른마스터")
            val hostB = createHost(other, "호스트B")
            // 일반 멤버로 B 에 추가
            mockMvc.post("/api/v2/hosts/$hostB/members") {
                with(auth(other))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("members" to listOf(mapOf("email" to teamA.guest.email(), "role" to "GUEST"))))
            }.andExpect { status { isOk() } }
            // 초대 대기(비활성)인 호스트 C 의 공연은 보이지 않는다
            val hostCMaster = newUser()
            val hostC = createHost(hostCMaster, "호스트C")
            hostRepository.findById(hostC).get().let {
                it.hostUsers.add(HostUser(host = it, userId = teamA.guest.id, role = HostRole.GUEST))
                hostRepository.save(it)
            }
            val hiddenC = createEvent(hostCMaster, hostC, "C공연")

            val a1 = createEvent(teamA.master, teamA.hostId, "A첫공연")
            val b1 = createEvent(other, hostB, "B공연")
            val a2 = createEvent(teamA.master, teamA.hostId, "A둘공연")
            val deleted = createEvent(teamA.master, teamA.hostId, "A삭제공연")
            mockMvc.delete("/api/v2/events/$deleted") { with(auth(teamA.master)) }.andExpect { status { isOk() } }
            patchBasic(teamA.master, a2, mapOf("place" to placeBody)).andExpect { status { isOk() } }
            fillBasicAndDetail(teamA.master, a1)
            saveTicket(a1)
            openV2(teamA.master, a1).andExpect { status { isOk() } }

            val before = LocalDate.now()
            val data = mockMvc.get("/api/v2/me/events") { with(auth(teamA.guest)) }
                .andExpect { status { isOk() } }.body().at("/data")
            val after = LocalDate.now()
            val ids = data["content"].map { it["eventId"].asLong() }
            assertEquals(listOf(a2, b1, a1), ids)
            assertFalse(ids.contains(hiddenC))
            assertEquals(3, data["totalElements"].asInt())

            val byId = data["content"].associateBy { it["eventId"].asLong() }
            assertEquals("호스트A", byId.getValue(a2)["hostName"].asText())
            assertEquals("호스트B", byId.getValue(b1)["hostName"].asText())
            assertEquals("롤링홀", byId.getValue(a2)["placeName"].asText())
            assertTrue(byId.getValue(b1)["placeName"].isNull)
            assertEquals("PREPARING", byId.getValue(b1)["displayStatus"].asText())
            assertTrue(byId.getValue(b1)["dDay"].isNull)
            assertEquals("UPCOMING", byId.getValue(a1)["displayStatus"].asText())
            // 자정 경계: 요청 전후 날짜 중 하나 기준이면 된다
            val expectedDDays = setOf(before, after).map { java.time.temporal.ChronoUnit.DAYS.between(it, baseStart.toLocalDate()) }
            assertTrue(byId.getValue(a1)["dDay"].asLong() in expectedDDays, byId.getValue(a1)["dDay"].toString())
            assertEquals(baseStart.plusMinutes(200).f(), byId.getValue(a1)["endAt"].asText())
        }

        @Test
        fun `항목마다 그 호스트에서 내 역할 myRole (MASTER, MANAGER, GUEST) (#726)`() {
            val me = newUser("나")
            val mine = createHost(me, "내호스트")
            val other = Team("남의호스트")
            val third = Team("셋째호스트")
            fun add(team: Team, role: String) = mockMvc.post("/api/v2/hosts/${team.hostId}/members") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("members" to listOf(mapOf("email" to me.email(), "role" to role))))
            }.andExpect { status { isOk() } }
            add(other, "MANAGER")
            add(third, "GUEST")
            val e1 = createEvent(me, mine, "내공연")
            val e2 = createEvent(other.master, other.hostId, "남의공연")
            val e3 = createEvent(third.master, third.hostId, "셋째공연")

            val roles = mockMvc.get("/api/v2/me/events") { with(auth(me)) }.andExpect { status { isOk() } }
                .body().at("/data/content").associate { it["eventId"].asLong() to it["myRole"].asText() }
            assertEquals(mapOf(e1 to "MASTER", e2 to "MANAGER", e3 to "GUEST"), roles)
            // 같은 공연도 보는 사람마다 역할이 다르다
            val otherView = mockMvc.get("/api/v2/me/events") { with(auth(other.guest)) }.andExpect { status { isOk() } }
                .body().at("/data/content").single()
            assertEquals(e2, otherView["eventId"].asLong())
            assertEquals("GUEST", otherView["myRole"].asText())
        }

        @Test
        fun `myRole 은 N+1 없음 - 호스트·공연 수와 관계없이 쿼리 수가 같다 (#726)`() {
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            // 통계는 컨텍스트 전체 공유라 앞 테스트의 비동기 알림 저장이 섞일 수 있다 → 3번 재고 최솟값 (잡음은 더하기만 한다)
            fun countQueries(user: User): Long = (1..3).minOf {
                statistics.isStatisticsEnabled = true
                try {
                    statistics.clear()
                    mockMvc.get("/api/v2/me/events") { with(auth(user)) }.andExpect { status { isOk() } }
                    statistics.prepareStatementCount
                } finally {
                    statistics.isStatisticsEnabled = false
                }
            }
            val small = Team("작은호스트")
            createEvent(small.master, small.hostId, "공연1")
            val one = countQueries(small.master)

            val me = newUser("나")
            val teams = (1..3).map { Team("호스트$it") }
            teams.forEachIndexed { i, team ->
                mockMvc.post("/api/v2/hosts/${team.hostId}/members") {
                    with(auth(team.master))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("members" to listOf(mapOf("email" to me.email(), "role" to if (i == 0) "MANAGER" else "GUEST"))))
                }.andExpect { status { isOk() } }
                repeat(2) { createEvent(team.master, team.hostId, "공연$i-$it") }
            }
            val many = countQueries(me)
            assertEquals(one, many, "호스트 1·공연 1: $one, 호스트 3·공연 6: $many")
        }

        @Test
        fun `keyword 는 공연명 부분일치, 페이지 크기 적용, 비로그인 401`() {
            val team = Team()
            createEvent(team.master, team.hostId, "봄 정기공연")
            val autumn = createEvent(team.master, team.hostId, "가을 정기공연")
            createEvent(team.master, team.hostId, "버스킹")

            val data = mockMvc.get("/api/v2/me/events?keyword=정기&size=1") { with(auth(team.guest)) }
                .andExpect { status { isOk() } }.body().at("/data")
            assertEquals(listOf(autumn), data["content"].map { it["eventId"].asLong() })
            assertEquals(2, data["totalElements"].asInt())
            assertTrue(data["hasNext"].asBoolean())

            // LIKE 와일드카드(%, _)는 문자 그대로 검색 (E-1, H-1)
            val percent = createEvent(team.master, team.hostId, "100%_라이브")
            fun ids(path: String, keyword: String, key: String): List<Long> =
                mockMvc.get(path) {
                    with(auth(team.guest))
                    param("keyword", keyword)
                }.andExpect { status { isOk() } }.body().at("/data/content").map { it[key].asLong() }
            assertEquals(listOf(percent), ids("/api/v2/me/events", "%", "eventId"))
            assertEquals(listOf(percent), ids("/api/v2/me/events", "_", "eventId"))
            val wildHost = createHost(team.guest, "a%b_c")
            createHost(team.guest, "abxc")
            assertEquals(listOf(wildHost), ids("/api/v2/me/hosts", "%b_", "hostId"))

            mockMvc.get("/api/v2/me/events?keyword=없는공연") { with(auth(team.guest)) }
                .andExpect { jsonPath("$.data.content.length()") { value(0) } }
            mockMvc.get("/api/v2/me/events").andExpect { status { isUnauthorized() } }
            mockMvc.get("/api/v2/me/events?size=51") { with(auth(team.guest)) }.andExpect { status { isBadRequest() } }
        }
    }

    // ===== E-3 / E-4 =====

    @Nested
    @DisplayName("E-3 어드민 상세 / E-4 기본 정보 수정")
    inner class ManageAndBasic {

        @Test
        fun `매니저가 포스터-이름-일정-장소-문의처-태그-티켓여부를 수정하면 상세에 그대로 보인다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val posterKey = V2ImageKeys.issued(presignedUrlService.eventImageKeyPrefix(eventId))
            val rock = tagId(TagCategory.GENRE, "락밴드")
            val regular = tagId(TagCategory.EVENT_TYPE, "정기공연")
            val hongdae = tagId(TagCategory.AREA, "홍대")

            patchBasic(
                team.manager,
                eventId,
                mapOf(
                    "posterImageKey" to posterKey,
                    "name" to " 새 이름 ",
                    "startAt" to baseStart.plusHours(1).f(),
                    "endAt" to baseStart.plusHours(3).f(),
                    "place" to placeBody,
                    "contacts" to listOf(
                        mapOf("type" to "INSTAGRAM", "value" to "@gosrock"),
                        mapOf("type" to "PHONE", "value" to "010-1234-5678"),
                    ),
                    "hasTicket" to false,
                    "tagIds" to listOf(hongdae, rock, regular, rock),
                ),
            ).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("새 이름") }
            }

            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.hostId") { value(team.hostId) }
                jsonPath("$.data.hostName") { value("고스락") }
                jsonPath("$.data.myRole") { value("GUEST") }
                jsonPath("$.data.posterImageKey") { value(posterKey) }
                jsonPath("$.data.posterImageUrl") { value(containsString(posterKey)) }
                jsonPath("$.data.name") { value("새 이름") }
                jsonPath("$.data.startAt") { value(baseStart.plusHours(1).f()) }
                jsonPath("$.data.endAt") { value(baseStart.plusHours(3).f()) }
                jsonPath("$.data.runTime") { value(120) }
                jsonPath("$.data.place.name") { value("롤링홀") }
                jsonPath("$.data.place.latitude") { value(37.548369) }
                jsonPath("$.data.contacts.length()") { value(2) }
                jsonPath("$.data.contacts[1].type") { value("PHONE") }
                jsonPath("$.data.hasTicket") { value(false) }
                // 분류 순(EVENT_TYPE → GENRE → AREA), 중복 제거
                jsonPath("$.data.tags.length()") { value(3) }
                jsonPath("$.data.tags[0].name") { value("정기공연") }
                jsonPath("$.data.tags[1].category") { value("GENRE") }
                jsonPath("$.data.tags[2].tagId") { value(hongdae) }
                jsonPath("$.data.checklist.basic") { value(true) }
                jsonPath("$.data.checklist.detail") { value(false) }
                jsonPath("$.data.checklist.ticketRequired") { value(false) }
            }

            // 중복 제거 후 개수로 검증: 원소 11개라도 중복 제거하면 1개
            patchBasic(team.manager, eventId, mapOf("tagIds" to List(11) { rock })).andExpect {
                status { isOk() }
                jsonPath("$.data.tags.length()") { value(1) }
            }
            // null 필드는 그대로, 빈 포스터는 제거, 태그 일부 교체
            patchBasic(team.manager, eventId, mapOf("posterImageKey" to "", "tagIds" to listOf(rock))).andExpect {
                status { isOk() }
                jsonPath("$.data.name") { value("새 이름") }
                jsonPath("$.data.posterImageKey") { value(null as Any?) }
                jsonPath("$.data.contacts.length()") { value(2) }
                jsonPath("$.data.tags.length()") { value(1) }
                jsonPath("$.data.tags[0].tagId") { value(rock) }
            }
        }

        @Test
        fun `잘못된 이미지 key, 없는 태그, 문의처 11개, 이름 26자, 종료가 시작 이전, 태그 11개는 400`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val otherEvent = createEvent(team.master, team.hostId)

            listOf(
                *V2ImageKeys.rejected(presignedUrlService.eventImageKeyPrefix(eventId), presignedUrlService.eventImageKeyPrefix(otherEvent))
                    .map { mapOf("posterImageKey" to it) to "Event_400_22" }.toTypedArray(),
                mapOf("tagIds" to listOf(987654321L)) to "Event_400_23",
                mapOf("endAt" to baseStart.minusMinutes(1).f()) to "Event_400_2",
                mapOf("startAt" to baseStart.plusMinutes(200).f()) to "Event_400_2",
            ).forEach { (body, code) ->
                patchBasic(team.master, eventId, body).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value(code) }
                }
            }
            listOf(
                mapOf("contacts" to (1..11).map { mapOf("type" to "ETC", "value" to "$it") }),
                mapOf("contacts" to listOf(mapOf("type" to "FAX", "value" to "1"))),
                mapOf("contacts" to listOf(mapOf("type" to "ETC", "value" to " "))),
                mapOf("name" to "가".repeat(26)),
                mapOf("name" to "  "),
                mapOf("tagIds" to (1L..11L).toList()),
                mapOf("place" to mapOf("name" to "롤링홀")),
                mapOf("contacts" to listOf(null)),
                mapOf("tagIds" to listOf(null)),
            ).forEach { body -> patchBasic(team.master, eventId, body).andExpect { status { isBadRequest() } } }
            // 요청 검증을 지나 도메인에서 걸리는 문의처 형식 오류는 Event_400_20 (#752): null 원소, 요청 검증(Java trim)은 통과하지만 trim 하면 빈 값(전각 공백)
            listOf(listOf(null), listOf(mapOf("type" to "ETC", "value" to "\u3000"))).forEach { contacts ->
                patchBasic(team.master, eventId, mapOf("contacts" to contacts)).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("Event_400_20") }
                }
            }

            // 실패한 요청은 아무것도 바꾸지 않는다 (트랜잭션 롤백)
            val event = eventRepository.findById(eventId).get()
            assertEquals("정기공연", event.getEventName())
            assertEquals(baseStart.plusMinutes(200), event.getEndAt())
        }

        @Test
        fun `일반 멤버 수정 403, 비멤버 상세 403, 다른 호스트 공연 IDOR 403, 없는 공연 404, 비로그인 401`() {
            val teamA = Team()
            val teamB = Team()
            val eventB = createEvent(teamB.master, teamB.hostId)

            patchBasic(teamA.guest, createEvent(teamA.master, teamA.hostId), mapOf("name" to "x"))
                .andExpect { status { isForbidden() } }
            mockMvc.get("/api/v2/events/$eventB/manage") { with(auth(teamA.outsider)) }.andExpect { status { isForbidden() } }
            // A 의 매니저가 B 의 공연을 건드리면 403
            mockMvc.get("/api/v2/events/$eventB/manage") { with(auth(teamA.master)) }.andExpect {
                status { isForbidden() }
                jsonPath("$.code") { value("HOST_400_2") }
            }
            patchBasic(teamA.master, eventB, mapOf("name" to "탈취")).andExpect { status { isForbidden() } }
            mockMvc.get("/api/v2/events/987654321/manage") { with(auth(teamA.master)) }.andExpect { status { isNotFound() } }
            mockMvc.get("/api/v2/events/$eventB/manage").andExpect { status { isUnauthorized() } }
            assertEquals("정기공연", eventRepository.findById(eventB).get().getEventName())
        }

        @Test
        fun `등록 후에도 기본 정보는 수정되지만 hasTicket 변경은 400, 같은 값은 허용`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            fillBasicAndDetail(team.master, eventId)
            saveTicket(eventId)
            openV2(team.master, eventId).andExpect { status { isOk() } }

            patchBasic(team.manager, eventId, mapOf("name" to "오픈 후 수정", "endAt" to baseStart.plusMinutes(100).f(), "hasTicket" to true))
                .andExpect {
                    status { isOk() }
                    jsonPath("$.data.name") { value("오픈 후 수정") }
                    jsonPath("$.data.runTime") { value(100) }
                    jsonPath("$.data.status") { value("OPEN") }
                }
            patchBasic(team.manager, eventId, mapOf("hasTicket" to false)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_18") }
            }
            // 등록된 공연의 시작 시각은 현재 이후로만 옮길 수 있다 (같은 값은 허용)
            val past = LocalDateTime.now().minusHours(1)
            patchBasic(team.manager, eventId, mapOf("startAt" to past.f(), "endAt" to past.plusHours(2).f())).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_24") }
            }
            patchBasic(team.manager, eventId, mapOf("startAt" to baseStart.f(), "name" to "같은 시작")).andExpect { status { isOk() } }
            patchBasic(team.manager, eventId, mapOf("startAt" to baseStart.plusDays(1).f(), "endAt" to baseStart.plusDays(1).plusHours(2).f()))
                .andExpect {
                    status { isOk() }
                    jsonPath("$.data.startAt") { value(baseStart.plusDays(1).f()) }
                    jsonPath("$.data.runTime") { value(120) }
                }
            // v1 PATCH basic 은 여전히 OPEN 이면 불가 (v1 동작 유지)
            v1PatchBasic(team.master, eventId, runTime = 30).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_4") }
            }
        }

        @Test
        fun `준비중 공연은 지난 시작 시각으로도 수정되고, 유효 티켓이 있으면 티켓 없음으로 못 바꾼다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val past = LocalDateTime.now().minusDays(1)
            patchBasic(team.master, eventId, mapOf("startAt" to past.f(), "endAt" to past.plusHours(1).f())).andExpect { status { isOk() } }

            val deleted = saveTicket(eventId, TicketItemStatus.DELETED)
            patchBasic(team.master, eventId, mapOf("hasTicket" to false)).andExpect { status { isOk() } }
            patchBasic(team.master, eventId, mapOf("hasTicket" to true)).andExpect { status { isOk() } }
            saveTicket(eventId)
            patchBasic(team.master, eventId, mapOf("hasTicket" to false)).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_25") }
            }
            assertTrue(eventRepository.findById(eventId).get().hasTicket)
            assertEquals(TicketItemStatus.DELETED, deleted.ticketItemStatus)
        }

        @Test
        fun `정산중, 지난공연은 기본 정보-섹션-이미지 수정 400`() {
            val team = Team()
            listOf(EventStatus.CALCULATING, EventStatus.CLOSED).forEach { status ->
                val eventId = createEvent(team.master, team.hostId)
                setStatus(eventId, status)
                patchBasic(team.master, eventId, mapOf("name" to "x")).andExpect { jsonPath("$.code") { value("Event_400_17") } }
                putSections(team.master, eventId, defaultSections).andExpect { jsonPath("$.code") { value("Event_400_17") } }
                mockMvc.post("/api/v2/events/$eventId/images") {
                    with(auth(team.master))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("purpose" to "POSTER", "extension" to "PNG"))
                }.andExpect { jsonPath("$.code") { value("Event_400_17") } }
                // 조회는 된다
                mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.guest)) }
                    .andExpect { jsonPath("$.data.displayStatus") { value("PAST") } }
            }
        }

        @Test
        fun `SUPER_ADMIN 비멤버는 상세를 볼 수 있고 myRole 은 null`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val admin = newUser().also { it.changeRole(AccountRole.SUPER_ADMIN) }.let { userRepository.save(it) }
            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(admin)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.myRole") { value(null as Any?) }
            }
        }
    }

    // ===== E-5 / E-6 =====

    @Nested
    @DisplayName("E-5 섹션 조회 / E-6 섹션 저장")
    inner class Sections {

        @Test
        fun `기본 4섹션 + 추가 섹션을 sortOrder 순으로 저장하고 다시 매긴다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val sections = defaultSections + mapOf("title" to " 게스트 ", "content" to "게스트 소개", "sortOrder" to -1)

            putSections(team.manager, eventId, sections).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(5) }
                jsonPath("$.data[0].title") { value("게스트") }
                jsonPath("$.data[0].sortOrder") { value(0) }
                jsonPath("$.data[1].title") { value("공연 소개") }
                jsonPath("$.data[4].title") { value("유의사항") }
                jsonPath("$.data[4].sortOrder") { value(4) }
                jsonPath("$.data[1].sectionId") { exists() }
                jsonPath("$.data[0].contentFormat") { value("HTML") }
            }
            // 제목과 무관하게 첫 섹션 본문이 v1 content
            assertEquals("게스트 소개", eventRepository.findById(eventId).get().eventDetail!!.content)

            // 전체 교체
            putSections(team.manager, eventId, listOf(mapOf("title" to "공연 소개", "content" to "교체", "sortOrder" to 0)))
                .andExpect { jsonPath("$.data.length()") { value(1) } }
            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].content") { value("교체") }
            }
        }

        @Test
        fun `HTML 본문은 sanitize 된다 - script, on 이벤트, javascript 링크, http 이미지 제거`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val dirty = "<p onclick=\"x()\">안녕<script>alert(1)</script></p>" +
                "<img src=\"https://cdn.dudoong.com/a.png\" onerror=\"alert(2)\">" +
                "<img src=\"http://evil.com/b.png\"><a href=\"javascript:alert(3)\">링크</a>" +
                "<a href=\"https://dudoong.com\">두둥</a><iframe src=\"https://evil.com\"></iframe>"

            val saved = putSections(team.master, eventId, listOf(mapOf("title" to "공연 소개", "content" to dirty, "sortOrder" to 0)))
                .andExpect { status { isOk() } }.body().at("/data/0/content").asText()

            listOf("<script", "alert(1)", "onclick", "onerror", "javascript:", "http://evil.com", "<iframe").forEach {
                assertFalse(saved.contains(it), "sanitize 누락 [$it]: $saved")
            }
            assertTrue(saved.contains("<p>안녕</p>"), saved)
            assertTrue(saved.contains("<img src=\"https://cdn.dudoong.com/a.png\">"), saved)
            assertTrue(saved.contains("<a href=\"https://dudoong.com\">두둥</a>"), saved)
            // v1 content 에도 sanitize 된 값이 들어간다
            assertEquals(saved, eventRepository.findById(eventId).get().eventDetail!!.content)
        }

        @Test
        fun `준비중 공연 섹션은 멤버만, 비로그인-비멤버는 404, 등록 후에는 비로그인도 조회`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            fillBasicAndDetail(team.master, eventId)

            mockMvc.get("/api/v2/events/$eventId/sections").andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("Event_404_1") }
            }
            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(team.outsider)) }.andExpect { status { isNotFound() } }
            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(team.guest)) }.andExpect { status { isOk() } }
            val admin = newUser().also { it.changeRole(AccountRole.SUPER_ADMIN) }.let { userRepository.save(it) }
            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(admin)) }.andExpect { status { isOk() } }

            saveTicket(eventId)
            openV2(team.master, eventId).andExpect { status { isOk() } }
            mockMvc.get("/api/v2/events/$eventId/sections").andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(4) }
                jsonPath("$.data[0].title") { value("공연 소개") }
                jsonPath("$.data[0].content") { value("<p>소개</p>") }
            }
            mockMvc.get("/api/v2/events/987654321/sections").andExpect { status { isNotFound() } }
            mockMvc.get("/api/v2/events/abc/sections").andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `섹션 0개, 11개, 제목 빈값-21자는 400, 일반 멤버 403, 다른 호스트 IDOR 403`() {
            val teamA = Team()
            val teamB = Team()
            val eventId = createEvent(teamA.master, teamA.hostId)
            listOf(
                emptyList(),
                (1..11).map { mapOf("title" to "s$it", "content" to "", "sortOrder" to it) },
                listOf(mapOf("title" to " ", "content" to "a", "sortOrder" to 0)),
                listOf(mapOf("title" to null, "content" to "a", "sortOrder" to 0)),
                listOf(mapOf("title" to "가".repeat(21), "content" to "a", "sortOrder" to 0)),
                listOf(null),
            ).forEach { body ->
                putSections(teamA.master, eventId, body).andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value("Event_400_21") }
                }
            }
            putSections(teamA.master, eventId, (1..10).map { mapOf("title" to "가".repeat(20), "content" to "", "sortOrder" to it) })
                .andExpect { status { isOk() } }
            putSections(teamA.guest, eventId, defaultSections).andExpect { status { isForbidden() } }
            putSections(teamB.master, eventId, defaultSections).andExpect { status { isForbidden() } }
        }
    }

    // ===== E-7 / E-8 =====

    @Nested
    @DisplayName("E-7 체크리스트 / E-8 등록")
    inner class ChecklistAndOpen {

        @Test
        fun `체크리스트는 기본 정보 → 상세 → 티켓 단계별로 채워지고 모두 충족되면 등록된다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)

            var c = checklist(team.guest, eventId)
            assertEquals(listOf(false, false, false, true, false), listOf(c["basic"], c["detail"], c["ticket"], c["ticketRequired"], c["canOpen"]).map { it.asBoolean() })
            openV2(team.master, eventId).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_7") }
            }

            // 장소만으로는 부족 (문의처 필요)
            patchBasic(team.master, eventId, mapOf("place" to placeBody)).andExpect { status { isOk() } }
            assertFalse(checklist(team.guest, eventId)["basic"].asBoolean())
            patchBasic(team.master, eventId, mapOf("contacts" to listOf(mapOf("type" to "EMAIL", "value" to "a@a.com"))))
            // 포스터 필수
            assertFalse(checklist(team.guest, eventId)["basic"].asBoolean())
            patchBasic(team.master, eventId, mapOf("posterImageKey" to posterKey(eventId))).andExpect { status { isOk() } }
            c = checklist(team.guest, eventId)
            assertTrue(c["basic"].asBoolean())
            assertFalse(c["detail"].asBoolean())

            // 본문이 빈 섹션만 있으면 미충족
            putSections(team.master, eventId, listOf(mapOf("title" to "공연 소개", "content" to "  ", "sortOrder" to 0)))
            assertFalse(checklist(team.guest, eventId)["detail"].asBoolean())
            putSections(team.master, eventId, defaultSections)
            c = checklist(team.guest, eventId)
            assertTrue(c["detail"].asBoolean())
            assertFalse(c["canOpen"].asBoolean())

            // 삭제된 티켓은 세지 않는다
            saveTicket(eventId, TicketItemStatus.DELETED)
            assertFalse(checklist(team.guest, eventId)["ticket"].asBoolean())
            openV2(team.master, eventId).andExpect { jsonPath("$.code") { value("Event_400_7") } }

            saveTicket(eventId)
            c = checklist(team.guest, eventId)
            assertTrue(c["ticket"].asBoolean())
            assertTrue(c["canOpen"].asBoolean())

            openV2(team.guest, eventId).andExpect { status { isForbidden() } }
            openV2(team.manager, eventId).andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("OPEN") }
                jsonPath("$.data.displayStatus") { value("UPCOMING") }
            }
            assertFalse(checklist(team.guest, eventId)["canOpen"].asBoolean())
            openV2(team.manager, eventId).andExpect { jsonPath("$.code") { value("Event_400_8") } }
        }

        @Test
        fun `티켓 없음 공연은 티켓 없이 v2 등록된다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId, hasTicket = false)
            fillBasicAndDetail(team.master, eventId)

            val c = checklist(team.master, eventId)
            assertFalse(c["ticket"].asBoolean())
            assertFalse(c["ticketRequired"].asBoolean())
            assertTrue(c["canOpen"].asBoolean())
            openV2(team.master, eventId).andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("OPEN") }
            }
        }

        @Test
        fun `시작 시각이 지난 공연은 등록 400, 다른 호스트 IDOR 403, 비멤버 체크리스트 403`() {
            val teamA = Team()
            val teamB = Team()
            val eventId = createEvent(teamA.master, teamA.hostId, hasTicket = false)
            fillBasicAndDetail(teamA.master, eventId)
            openV2(teamB.master, eventId).andExpect { status { isForbidden() } }
            mockMvc.get("/api/v2/events/$eventId/checklist") { with(auth(teamA.outsider)) }.andExpect { status { isForbidden() } }

            val event = eventRepository.findById(eventId).get()
            v2EventDomainService.updateBasic(event, startAt = LocalDateTime.now().minusHours(1).withSecond(0).withNano(0))
            eventRepository.save(event)
            assertFalse(checklist(teamA.master, eventId)["canOpen"].asBoolean())
            openV2(teamA.master, eventId).andExpect { jsonPath("$.code") { value("Event_400_15") } }
        }
    }

    // ===== E-9 / E-10 / E-11 =====

    @Nested
    @DisplayName("E-9 삭제 / E-10 이미지 / E-11 태그")
    inner class DeleteImagesTags {

        @Test
        fun `준비중 공연만 삭제되고 삭제 후 404, 등록된 공연은 400, 일반 멤버 403`() {
            val team = Team()
            val preparing = createEvent(team.master, team.hostId)
            mockMvc.delete("/api/v2/events/$preparing") { with(auth(team.guest)) }.andExpect { status { isForbidden() } }
            mockMvc.delete("/api/v2/events/$preparing") { with(auth(team.manager)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.status") { value("DELETED") }
            }
            mockMvc.get("/api/v2/events/$preparing/manage") { with(auth(team.master)) }.andExpect { status { isNotFound() } }
            mockMvc.delete("/api/v2/events/$preparing") { with(auth(team.master)) }.andExpect { status { isNotFound() } }

            val opened = createEvent(team.master, team.hostId, hasTicket = false)
            fillBasicAndDetail(team.master, opened)
            openV2(team.master, opened).andExpect { status { isOk() } }
            mockMvc.delete("/api/v2/events/$opened") { with(auth(team.master)) }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_19") }
            }
            val closed = createEvent(team.master, team.hostId)
            setStatus(closed, EventStatus.CLOSED)
            mockMvc.delete("/api/v2/events/$closed") { with(auth(team.master)) }
                .andExpect { jsonPath("$.code") { value("Event_400_19") } }
        }

        @Test
        fun `매니저는 포스터-본문 업로드 url 을 받고 key 는 이 공연 경로, 일반 멤버 403`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            listOf("POSTER", "SECTION").forEach { purpose ->
                val key = mockMvc.post("/api/v2/events/$eventId/images") {
                    with(auth(team.manager))
                    contentType = MediaType.APPLICATION_JSON
                    content = json(mapOf("purpose" to purpose, "extension" to "PNG"))
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.purpose") { value(purpose) }
                    jsonPath("$.data.presignedUrl") { exists() }
                    jsonPath("$.data.url") { exists() }
                }.body().at("/data/key").asText()
                assertTrue(key.startsWith(presignedUrlService.eventImageKeyPrefix(eventId)), key)
                // 발급받은 key 는 포스터로 저장된다
                patchBasic(team.manager, eventId, mapOf("posterImageKey" to key)).andExpect { status { isOk() } }
            }
            mockMvc.post("/api/v2/events/$eventId/images") {
                with(auth(team.guest))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("purpose" to "POSTER", "extension" to "PNG"))
            }.andExpect { status { isForbidden() } }
        }

        @Test
        fun `태그 목록은 비로그인도 분류 순으로 받는다`() {
            val data = mockMvc.get("/api/v2/tags").andExpect { status { isOk() } }.body().at("/data")
            assertEquals(listOf("EVENT_TYPE", "GENRE", "AREA", "TEAM"), data.map { it["category"].asText() })
            SEED_TAGS.forEach { (category, names) ->
                val group = data.first { it["category"].asText() == category.name }
                assertEquals(names, group["tags"].map { it["name"].asText() })
                assertTrue(group["tags"].all { it["tagId"].asLong() > 0 })
            }
        }
    }

    // ===== v1 호환 =====

    private fun v1PatchBasic(requester: User, eventId: Long, runTime: Long, startAt: LocalDateTime = baseStart): ResultActionsDsl =
        mockMvc.patch("/api/v1/events/$eventId/basic") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(
                mapOf(
                    "name" to "v1수정",
                    "startAt" to startAt.f(),
                    "runTime" to runTime,
                    "placeName" to "v1공연장",
                    "placeAddress" to "v1주소",
                    "longitude" to 126.9,
                    "latitude" to 37.5,
                ),
            )
        }

    private fun v1PatchDetails(requester: User, eventId: Long, detailContent: String): ResultActionsDsl =
        mockMvc.patch("/api/v1/events/$eventId/details") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("posterImageKey" to posterKey(eventId), "content" to detailContent))
        }

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 로 저장한 일정-공연 소개가 v1 상세의 runTime-endAt-content 와 일치한다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            patchBasic(team.master, eventId, mapOf("startAt" to baseStart.plusMinutes(30).f(), "endAt" to baseStart.plusMinutes(140).f()))
            putSections(team.master, eventId, defaultSections.reversed()).andExpect { status { isOk() } }

            mockMvc.get("/api/v1/events/$eventId") { with(auth(team.master)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.startAt") { value(baseStart.plusMinutes(30).f()) }
                jsonPath("$.data.runTime") { value(110) }
                jsonPath("$.data.endAt") { value(baseStart.plusMinutes(140).f()) }
                jsonPath("$.data.content") { value("<p>소개</p>") }
            }
            // v1 응답 형식 그대로 (v2 필드 노출 없음)
            val data = mockMvc.get("/api/v1/events/$eventId") { with(auth(team.master)) }.body().at("/data")
            listOf("hasTicket", "sections", "contacts", "tags", "storedEndAt").forEach { assertFalse(data.has(it), "v1 상세에 $it 노출") }
        }

        @Test
        fun `end_at 컬럼이 어긋나도 v1, v2 응답과 종료 배치 조회는 startAt + runTime 기준이다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            val event = eventRepository.findById(eventId).get()
            ReflectionTestUtils.setField(event, "storedEndAt", baseStart.plusDays(10))
            eventRepository.save(event)

            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.master)) }
                .andExpect { jsonPath("$.data.endAt") { value(baseStart.plusMinutes(200).f()) } }
            mockMvc.get("/api/v1/events/$eventId") { with(auth(team.master)) }
                .andExpect { jsonPath("$.data.endAt") { value(baseStart.plusMinutes(200).f()) } }
            setStatus(eventId, EventStatus.OPEN)
            // 종료 배치 조회(TIMESTAMPADD(run_time, start_at))도 같은 기준: 계산 종료 직후 시각이면 대상, 직전이면 아님
            val endedIds = { time: LocalDateTime -> eventRepository.queryEventsByEndAtBeforeAndStatusOpen(time).map { it.id } }
            assertTrue(eventId in endedIds(baseStart.plusMinutes(201)))
            assertFalse(eventId in endedIds(baseStart.plusMinutes(199)))
        }

        @Test
        fun `v1 PATCH basic, details 로 바꾸면 v2 상세 endAt 과 공연 소개 섹션에 반영된다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId)
            putSections(team.master, eventId, defaultSections).andExpect { status { isOk() } }

            v1PatchBasic(team.master, eventId, runTime = 45).andExpect { status { isOk() } }
            v1PatchDetails(team.master, eventId, "v1 본문").andExpect { status { isOk() } }

            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.master)) }.andExpect {
                jsonPath("$.data.name") { value("v1수정") }
                jsonPath("$.data.endAt") { value(baseStart.plusMinutes(45).f()) }
                jsonPath("$.data.runTime") { value(45) }
                jsonPath("$.data.place.name") { value("v1공연장") }
            }
            assertEquals(baseStart.plusMinutes(45), eventRepository.findById(eventId).get().storedEndAt)
            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(team.master)) }.andExpect {
                jsonPath("$.data.length()") { value(4) }
                jsonPath("$.data[0].content") { value("v1 본문") }
                jsonPath("$.data[0].contentFormat") { value("MARKDOWN") }
                jsonPath("$.data[1].content") { value("예매") }
                jsonPath("$.data[1].contentFormat") { value("HTML") }
            }
        }

        @Test
        fun `v1 로 만든 섹션 없는 기존 공연은 content 를 공연 소개로 대체 표시하고 상세 항목을 충족한다`() {
            val team = Team()
            val eventId = mockMvc.post("/api/v1/events") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("hostId" to team.hostId, "name" to "v1공연", "startAt" to baseStart.f(), "runTime" to 90))
            }.andExpect { status { isOk() } }.body().at("/data/eventId").asLong()
            v1PatchDetails(team.master, eventId, "v1 기존 본문").andExpect { status { isOk() } }

            mockMvc.get("/api/v2/events/$eventId/sections") { with(auth(team.guest)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].sectionId") { value(null as Any?) }
                jsonPath("$.data[0].title") { value("공연 소개") }
                jsonPath("$.data[0].content") { value("v1 기존 본문") }
                jsonPath("$.data[0].contentFormat") { value("MARKDOWN") }
            }
            mockMvc.get("/api/v2/events/$eventId/manage") { with(auth(team.guest)) }.andExpect {
                jsonPath("$.data.endAt") { value(baseStart.plusMinutes(90).f()) }
                jsonPath("$.data.hasTicket") { value(true) }
                jsonPath("$.data.checklist.detail") { value(true) }
            }
        }

        @Test
        fun `v1 open 은 hasTicket=false 여도 티켓을 요구하고, v1 체크리스트 응답은 그대로다`() {
            val team = Team()
            val eventId = createEvent(team.master, team.hostId, hasTicket = false)
            v1PatchBasic(team.master, eventId, runTime = 60).andExpect { status { isOk() } }
            v1PatchDetails(team.master, eventId, "본문").andExpect { status { isOk() } }

            mockMvc.patch("/api/v1/events/$eventId/open") { with(auth(team.master)) }.andExpect { status { isBadRequest() } }
            assertEquals(EventStatus.PREPARING, eventRepository.findById(eventId).get().status)

            mockMvc.get("/api/v1/events/$eventId/checklist") { with(auth(team.master)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.hasBasic") { value(true) }
                jsonPath("$.data.hasDetail") { value(true) }
                jsonPath("$.data.hasTicketItem") { value(false) }
                jsonPath("$.data.ticketRequired") { doesNotExist() }
            }
        }
    }

    companion object {
        /** V002 [DATA] 시드와 같은 값 */
        val SEED_TAGS = TagSeed.DEFAULT
    }
}
