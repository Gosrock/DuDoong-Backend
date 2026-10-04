package band.gosrock.api.v2.event

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.ticket.V2TicketApiTestSupport
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.tag.domain.Tag
import band.gosrock.domain.domains.tag.domain.TagCategory
import band.gosrock.domain.domains.tag.domain.TagSeed
import band.gosrock.domain.domains.tag.repository.TagRepository
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * v2 사용자 앱 공연 탐색 API 통합 테스트 (#716): P-1 ~ P-5, 모두 비로그인.
 * 컨텍스트(H2)를 다른 테스트와 공유하므로 리스트는 테스트마다 다른 토큰을 공연명·호스트명에 넣고 keyword 로 격리한다.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공연 탐색 API")
class V2EventBrowseControllerTest : V2TicketApiTestSupport() {

    @Autowired private lateinit var tagRepository: TagRepository

    @Autowired private lateinit var presignedUrlService: S3UploadPresignedUrlService

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private val now: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES)

    @BeforeEach
    fun seedTags() {
        TagSeed.DEFAULT.forEach { (category, names) ->
            names.forEachIndexed { index, name ->
                if (!tagRepository.existsByCategoryAndName(category, name)) tagRepository.save(Tag(category = category, name = name, sortOrder = index))
            }
        }
    }

    private fun tagId(category: TagCategory, name: String): Long =
        tagRepository.findAll().first { it.category == category && it.name == name }.id!!

    private val 정기 by lazy { tagId(TagCategory.EVENT_TYPE, "정기공연") }
    private val 단독 by lazy { tagId(TagCategory.EVENT_TYPE, "단독공연") }
    private val 락 by lazy { tagId(TagCategory.GENRE, "락밴드") }
    private val 힙합 by lazy { tagId(TagCategory.GENRE, "힙합") }
    private val 홍대 by lazy { tagId(TagCategory.AREA, "홍대") }

    private fun token(): String = UUID.randomUUID().toString().replace("-", "").take(6)

    // ===== fixtures =====

    private fun createEvent(master: User, hostId: Long, name: String, hasTicket: Boolean = true): Long =
        mockMvc.post("/api/v2/events") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("hostId" to hostId, "name" to name, "startAt" to eventStart.f(), "endAt" to eventStart.plusHours(2).f(), "hasTicket" to hasTicket))
        }.andExpect { status { isOk() } }.data().at("/eventId").asLong()

    private fun patchBasic(master: User, eventId: Long, body: Map<String, Any?>) {
        mockMvc.patch("/api/v2/events/$eventId/basic") {
            with(auth(master))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }.andExpect { status { isOk() } }
    }

    private fun posterKey(eventId: Long) = "${presignedUrlService.eventImageKeyPrefix(eventId)}poster.png"

    private val placeBody = mapOf("name" to "롤링홀", "address" to "서울 마포구 어울마당로 35", "latitude" to 37.548369, "longitude" to 126.920036)

    /** 상태·시작 시각을 직접 지정 (체크리스트·배치 없이). runTime 120분 */
    private fun setSchedule(eventId: Long, status: EventStatus, startAt: LocalDateTime, runTime: Long = 120) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = event.getEventName(), startAt = startAt, runTime = runTime))
        ReflectionTestUtils.setField(event, "status", status)
        eventRepository.save(event)
    }

    /** 공연 하나: 포스터·장소·태그(+문의처) 채우고 상태·시작 지정 */
    private fun event(
        master: User,
        hostId: Long,
        name: String,
        status: EventStatus,
        startAt: LocalDateTime,
        tags: List<Long> = emptyList(),
        contacts: List<Map<String, String>>? = null,
    ): Long {
        val id = createEvent(master, hostId, name)
        patchBasic(master, id, mapOf("posterImageKey" to posterKey(id), "place" to placeBody, "tagIds" to tags, "contacts" to contacts))
        setSchedule(id, status, startAt)
        return id
    }

    private fun anonymousGet(path: String, params: Map<String, String> = emptyMap()): ResultActionsDsl =
        mockMvc.get(path) { params.forEach { (k, v) -> param(k, v) } }

    private fun list(keyword: String, tagIds: List<Long> = emptyList(), includePast: Boolean = true, page: Int = 0, size: Int = 50): JsonNode {
        val params = mutableMapOf("keyword" to keyword, "includePast" to includePast.toString(), "page" to page.toString(), "size" to size.toString())
        if (tagIds.isNotEmpty()) params["tagIds"] = tagIds.joinToString(",")
        return anonymousGet("/api/v2/events", params).andExpect { status { isOk() } }.data()
    }

    private fun JsonNode.ids(): List<Long> = at("/content").map { it.at("/eventId").asLong() }

    /** 리스트 시나리오: 호스트 2개(호스트명에 토큰), 다가오는·지난·준비중·삭제 공연 */
    private inner class World {
        val tok = token()
        val master = newUser("마스터")
        val otherMaster = newUser("다른마스터")
        val hostId = createHost(master, "고스락$tok")
        val bandHostId = createHost(otherMaster, "밴드$tok")

        val soon2 = event(master, hostId, "공연$tok-2", EventStatus.OPEN, now.plusDays(2), listOf(정기, 홍대))
        val soon1 = event(master, hostId, "공연$tok-1", EventStatus.OPEN, now.plusDays(1), listOf(단독, 힙합))
        val soon3 = event(master, hostId, "공연$tok-3", EventStatus.OPEN, now.plusDays(3), listOf(정기, 락))
        // 공연명에는 토큰이 없고 호스트명에만 있음
        val bandSoon4 = event(otherMaster, bandHostId, "다른공연", EventStatus.OPEN, now.plusDays(4))
        // 시작 1시간 전 ~ 종료(시작 + 120분) 전: 진행 중 / 종료가 지났지만 종료 배치 전인 OPEN
        val started = event(master, hostId, "공연$tok-시작", EventStatus.OPEN, now.minusHours(1))
        val ended = event(master, hostId, "공연$tok-끝", EventStatus.OPEN, now.minusHours(5))
        val calculating = event(master, hostId, "공연$tok-정산", EventStatus.CALCULATING, now.minusDays(2))
        val closed = event(master, hostId, "공연$tok-종료", EventStatus.CLOSED, now.minusDays(10), listOf(정기, 락))
        val preparing = event(master, hostId, "공연$tok-준비", EventStatus.PREPARING, now.plusDays(1), listOf(정기, 락))
        val deleted = event(master, hostId, "공연$tok-삭제", EventStatus.DELETED, now.plusDays(1), listOf(정기, 락))
    }

    // ===== P-1 =====

    @Nested
    @DisplayName("P-1 홈")
    inner class Home {

        @Test
        fun `비로그인 - 종료 전 등록 공연(진행 중 + 시작 전) 임박순, 준비중·삭제·종료·지난 공연 제외, 최대 10개`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "홈$tok")
            val b = event(master, hostId, "홈$tok-b", EventStatus.OPEN, now.plusMinutes(30))
            val a = event(master, hostId, "홈$tok-a", EventStatus.OPEN, now.plusMinutes(20))
            // 시작 10분 전 ~ 종료(시작 + 120분) 전: 진행 중
            val ongoing = event(master, hostId, "홈$tok-진행", EventStatus.OPEN, now.minusMinutes(10))
            val ended = event(master, hostId, "홈$tok-끝", EventStatus.OPEN, now.minusHours(3))
            val preparing = event(master, hostId, "홈$tok-준비", EventStatus.PREPARING, now.plusMinutes(10))
            val deleted = event(master, hostId, "홈$tok-삭제", EventStatus.DELETED, now.plusMinutes(10))
            val closed = event(master, hostId, "홈$tok-종료", EventStatus.CLOSED, now.plusMinutes(10))

            val events = anonymousGet("/api/v2/home").andExpect { status { isOk() } }.data().at("/events")
            assertTrue(events.size() in 1..10)
            val ids = events.map { it.at("/eventId").asLong() }
            // 공유 컨텍스트라 다른 테스트 공연이 섞인다: 절대 위치 대신 상대 순서·포함 여부만 본다
            listOf(preparing, deleted, ended, closed).forEach { assertFalse(it in ids, "노출되면 안 되는 공연 $it: $ids") }
            val mine = ids.filter { it in listOf(ongoing, a, b) }
            assertEquals(listOf(ongoing, a, b).take(mine.size), mine, "진행 중 → 임박순: $ids")
            // 홈 = 기본 리스트(includePast=false) 앞부분과 같은 기준·순서
            val defaultList = anonymousGet("/api/v2/events", mapOf("size" to "10")).andExpect { status { isOk() } }.data()
            assertEquals(defaultList.ids(), ids)
            // 같은 조건을 검색어로 격리해 확인
            assertEquals(listOf(ongoing, a, b), list(tok, includePast = false).ids())
            // 항목 표시 상태: 시작 전 UPCOMING, 시작했으면 ONGOING
            events.forEach {
                val startAt = LocalDateTime.parse(it.at("/startAt").asText(), java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
                assertEquals(if (startAt.isAfter(LocalDateTime.now())) "UPCOMING" else "ONGOING", it.at("/displayStatus").asText())
            }

            val item = list(tok, includePast = false).at("/content").first { it.at("/eventId").asLong() == a }
            assertEquals("홈$tok", item.at("/hostName").asText())
            val homeItem = events.firstOrNull { it.at("/eventId").asLong() == a }
            if (homeItem != null) {
                assertEquals("홈$tok-a", homeItem.at("/name").asText())
                assertEquals("홈$tok", homeItem.at("/hostName").asText())
                assertEquals("롤링홀", homeItem.at("/placeName").asText())
                assertTrue(homeItem.at("/posterImageUrl").asText().endsWith("poster.png"))
            }
        }
    }

    // ===== P-2 =====

    @Nested
    @DisplayName("P-2 공연 리스트")
    inner class Search {

        @Test
        fun `비로그인 - includePast=true 면 진행 중·다가오는 공연 임박순 후 지난 공연 최근순, 준비중·삭제 제외`() {
            val w = World()
            val result = list(w.tok, includePast = true)
            assertEquals(listOf(w.started, w.soon1, w.soon2, w.soon3, w.bandSoon4, w.ended, w.calculating, w.closed), result.ids())
            val statuses = result.at("/content").map { it.at("/displayStatus").asText() }
            assertEquals(listOf("ONGOING", "UPCOMING", "UPCOMING", "UPCOMING", "UPCOMING", "PAST", "PAST", "PAST"), statuses)
            assertEquals(8, result.at("/totalElements").asInt())
        }

        @Test
        fun `includePast=false(기본) 면 종료 전 등록 공연만 - 진행 중이 맨 앞, 종료된 OPEN 은 제외`() {
            val w = World()
            val active = list(w.tok, includePast = false)
            assertEquals(listOf(w.started, w.soon1, w.soon2, w.soon3, w.bandSoon4), active.ids())
            assertEquals(listOf("ONGOING", "UPCOMING", "UPCOMING", "UPCOMING", "UPCOMING"), active.at("/content").map { it.at("/displayStatus").asText() })
            assertEquals(5, active.at("/totalElements").asInt())
            // 파라미터 생략 = false
            val defaults = anonymousGet("/api/v2/events", mapOf("keyword" to w.tok)).andExpect { status { isOk() } }.data()
            assertEquals(listOf(w.started, w.soon1, w.soon2, w.soon3, w.bandSoon4), defaults.ids())
        }

        @Test
        fun `항목 - 호스트명, 태그(분류 순), 종료 시각, 장소, 포스터`() {
            val w = World()
            val item = list(w.tok).at("/content").first { it.at("/eventId").asLong() == w.soon2 }
            assertEquals("공연${w.tok}-2", item.at("/name").asText())
            assertEquals("고스락${w.tok}", item.at("/hostName").asText())
            assertEquals(now.plusDays(2).f(), item.at("/startAt").asText())
            assertEquals(now.plusDays(2).plusMinutes(120).f(), item.at("/endAt").asText())
            assertEquals("롤링홀", item.at("/placeName").asText())
            assertTrue(item.at("/posterImageUrl").asText().endsWith("poster.png"))
            assertEquals(listOf("EVENT_TYPE:정기공연", "AREA:홍대"), item.at("/tags").map { "${it.at("/category").asText()}:${it.at("/name").asText()}" })
            assertEquals(listOf(정기, 홍대), item.at("/tags").map { it.at("/tagId").asLong() })
            val band = list(w.tok).at("/content").first { it.at("/eventId").asLong() == w.bandSoon4 }
            assertEquals("밴드${w.tok}", band.at("/hostName").asText())
            assertEquals(0, band.at("/tags").size())
        }

        @Test
        fun `keyword - 공연명 OR 호스트명 부분일치, 대소문자 무시`() {
            val w = World()
            // 호스트명만 일치
            assertEquals(listOf(w.bandSoon4), list("밴드${w.tok}").ids())
            // 공연명만 일치 (호스트명 '고스락{tok}' 에는 '-1' 이 없다)
            assertEquals(listOf(w.soon1), list("${w.tok}-1").ids())
            assertEquals(list(w.tok).ids(), list(w.tok.uppercase()).ids())
            assertEquals(0, list("없는${w.tok}").at("/content").size())
        }

        @Test
        fun `keyword 특수문자 - % _ ! 는 문자 그대로 검색`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "특수$tok")
            val percent = event(master, hostId, "50%_$tok", EventStatus.OPEN, now.plusDays(1))
            val bang = event(master, hostId, "A!$tok", EventStatus.OPEN, now.plusDays(2))
            val plain = event(master, hostId, "5000x$tok", EventStatus.OPEN, now.plusDays(3))

            assertEquals(listOf(percent, bang, plain), list(tok).ids())
            // 와일드카드로 해석되면 plain('5000x') 도 걸린다
            assertEquals(listOf(percent), list("%_$tok").ids())
            assertEquals(listOf(percent), list("0%_").ids().filter { it in listOf(percent, bang, plain) })
            assertEquals(listOf(bang), list("!$tok").ids())
        }

        @Test
        fun `tagIds - 같은 분류 OR, 분류끼리 AND, 3개 분류`() {
            val w = World()
            // 한 개
            assertEquals(listOf(w.soon2, w.soon3, w.closed), list(w.tok, listOf(정기)).ids())
            // 같은 분류 OR
            assertEquals(listOf(w.soon1, w.soon2, w.soon3, w.closed), list(w.tok, listOf(정기, 단독)).ids())
            // 분류끼리 AND
            assertEquals(listOf(w.soon3, w.closed), list(w.tok, listOf(정기, 락)).ids())
            // (정기 OR 단독) AND (락 OR 힙합)
            assertEquals(listOf(w.soon1, w.soon3, w.closed), list(w.tok, listOf(정기, 단독, 락, 힙합)).ids())
            // 3개 분류 모두 만족하는 공연 없음
            assertEquals(emptyList<Long>(), list(w.tok, listOf(정기, 락, 홍대)).ids())
            assertEquals(listOf(w.soon2), list(w.tok, listOf(정기, 홍대)).ids())
            // includePast=false 와 조합
            assertEquals(listOf(w.soon3), list(w.tok, listOf(정기, 락), includePast = false).ids())
            // 중복 id 는 하나로
            assertEquals(listOf(w.soon3, w.closed), list(w.tok, listOf(정기, 락, 락)).ids())
        }

        @Test
        fun `없는 태그 id·잘못된 파라미터는 400`() {
            val w = World()
            anonymousGet("/api/v2/events", mapOf("keyword" to w.tok, "tagIds" to "$정기,999999")).andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("Event_400_23") }
            }
            anonymousGet("/api/v2/events", mapOf("tagIds" to "abc")).andExpect { status { isBadRequest() } }
            anonymousGet("/api/v2/events", mapOf("sort" to "LATEST")).andExpect { status { isBadRequest() } }
            anonymousGet("/api/v2/events", mapOf("size" to "51")).andExpect { status { isBadRequest() } }
            anonymousGet("/api/v2/events", mapOf("page" to "-1")).andExpect { status { isBadRequest() } }
            // 검색어 50자 / 태그 id 50개 상한 — 요청 검증 400 (없는 태그 id 의 Event_400_23 과 다른 코드)
            anonymousGet("/api/v2/events", mapOf("keyword" to "가".repeat(50))).andExpect { status { isOk() } }
            anonymousGet("/api/v2/events", mapOf("keyword" to "가".repeat(51))).andExpect { status { isBadRequest() } }
            val tooMany = anonymousGet("/api/v2/events", mapOf("tagIds" to (1..51).joinToString(","))).andExpect { status { isBadRequest() } }.body()
            assertTrue(tooMany.at("/code").asText() != "Event_400_23", "개수 초과 코드: ${tooMany.at("/code").asText()}")
            anonymousGet("/api/v2/events", mapOf("keyword" to w.tok, "tagIds" to List(50) { 정기 }.joinToString(","))).andExpect { status { isOk() } }
            anonymousGet("/api/v2/events", mapOf("sort" to "UPCOMING", "keyword" to w.tok)).andExpect { status { isOk() } }
        }

        @Test
        fun `페이징 - 정렬 순서대로 나뉘고 전체 건수·다음 페이지 여부`() {
            val w = World()
            val all = list(w.tok).ids()
            val p0 = list(w.tok, size = 3, page = 0)
            val p1 = list(w.tok, size = 3, page = 1)
            val p2 = list(w.tok, size = 3, page = 2)
            assertEquals(all, p0.ids() + p1.ids() + p2.ids())
            assertEquals(listOf(3, 3, 2), listOf(p0, p1, p2).map { it.at("/content").size() })
            assertEquals(8, p0.at("/totalElements").asInt())
            assertEquals(3, p0.at("/totalPages").asInt())
            assertTrue(p0.at("/hasNext").asBoolean())
            assertFalse(p2.at("/hasNext").asBoolean())
            assertEquals(3, p2.at("/size").asInt())
            assertEquals(2, p2.at("/page").asInt())
        }

        @Test
        fun `같은 시작 시각이면 id 순 (다가오는·지난 공연 모두)`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "동시$tok")
            val u1 = event(master, hostId, "동시$tok-u1", EventStatus.OPEN, now.plusDays(5))
            val u2 = event(master, hostId, "동시$tok-u2", EventStatus.OPEN, now.plusDays(5))
            val p1 = event(master, hostId, "동시$tok-p1", EventStatus.CLOSED, now.minusDays(5))
            val p2 = event(master, hostId, "동시$tok-p2", EventStatus.CLOSED, now.minusDays(5))
            assertEquals(listOf(u1, u2, p1, p2), list(tok).ids())
        }

        @Test
        fun `N+1 없음 - 페이지 크기와 관계없이 쿼리 수가 같다 (태그·호스트명 일괄 조회)`() {
            val w = World()
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            statistics.isStatisticsEnabled = true
            try {
                fun countQueries(size: Int): Long {
                    statistics.clear()
                    list(w.tok, listOf(정기, 단독, 락, 힙합), size = size)
                    return statistics.prepareStatementCount
                }
                // 결과 3건(soon1, soon3, closed). 1·2건 페이지는 건수 쿼리를 하고, 한 페이지에 다 들어오면 건수 쿼리를 생략한다
                val one = countQueries(1)
                val two = countQueries(2)
                val all = countQueries(50)
                assertEquals(one, two, "size=1: $one, size=2: $two")
                // 태그 검증 1 + 목록 1 + 건수 1 + 공연 태그 1 + 태그 1 + 호스트명 1
                assertEquals(6, two)
                assertEquals(5, all)
            } finally {
                statistics.isStatisticsEnabled = false
            }
        }

        @Test
        fun `준비중 공연은 태그·검색어가 맞아도 절대 노출되지 않는다`() {
            val w = World()
            val all = list(w.tok, listOf(정기, 락)).ids() + list("${w.tok}-준비").ids() + list("${w.tok}-삭제").ids()
            assertFalse(w.preparing in all)
            assertFalse(w.deleted in all)
        }
    }

    // ===== P-3 / P-4 =====

    @Nested
    @DisplayName("P-3 공개 상세 / P-4 섹션")
    inner class Detail {

        @Test
        fun `비로그인 - 기본 정보, 태그(분류 포함), 호스트 요약, 공연 문의처, UPCOMING`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "상세$tok")
            val id = event(
                master, hostId, "상세$tok", EventStatus.OPEN, now.plusDays(1),
                tags = listOf(락, 정기), contacts = listOf(mapOf("type" to "INSTAGRAM", "value" to "@gosrock"), mapOf("type" to "PHONE", "value" to "010-1234-5678")),
            )
            val body = anonymousGet("/api/v2/events/$id").andExpect { status { isOk() } }.data()
            assertEquals(id, body.at("/eventId").asLong())
            assertEquals("상세$tok", body.at("/name").asText())
            assertTrue(body.at("/posterImageUrl").asText().endsWith("poster.png"))
            assertEquals(now.plusDays(1).f(), body.at("/startAt").asText())
            assertEquals(now.plusDays(1).plusMinutes(120).f(), body.at("/endAt").asText())
            assertEquals(120, body.at("/runTime").asInt())
            assertEquals("롤링홀", body.at("/place/name").asText())
            assertEquals("서울 마포구 어울마당로 35", body.at("/place/address").asText())
            assertEquals(37.548369, body.at("/place/latitude").asDouble())
            assertEquals(126.920036, body.at("/place/longitude").asDouble())
            assertTrue(body.at("/hasTicket").asBoolean())
            // 분류 순(EVENT_TYPE → GENRE)
            assertEquals(listOf("EVENT_TYPE:정기공연", "GENRE:락밴드"), body.at("/tags").map { "${it.at("/category").asText()}:${it.at("/name").asText()}" })
            assertEquals(hostId, body.at("/host/hostId").asLong())
            assertEquals("상세$tok", body.at("/host/name").asText())
            assertTrue(body.at("/host").has("profileImageUrl"))
            assertEquals(listOf("INSTAGRAM:@gosrock", "PHONE:010-1234-5678"), body.at("/contacts").map { "${it.at("/type").asText()}:${it.at("/value").asText()}" })
            assertEquals("UPCOMING", body.at("/displayStatus").asText())
            // 내부 정보 미노출
            listOf("status", "checkInToken", "myRole", "checklist", "posterImageKey", "hostId").forEach { assertFalse(body.has(it), "노출 금지 필드 $it") }
            listOf("slackUrl", "members", "masterUserId", "partner").forEach { assertFalse(body.at("/host").has(it), "노출 금지 호스트 필드 $it") }
        }

        @Test
        fun `공연 문의처가 없으면 호스트 연락처로 대체`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "대체$tok")
            val id = event(master, hostId, "대체$tok", EventStatus.OPEN, now.plusDays(1))
            anonymousGet("/api/v2/events/$id").andExpect {
                status { isOk() }
                jsonPath("$.data.contacts.length()") { value(1) }
                jsonPath("$.data.contacts[0].type") { value("EMAIL") }
                jsonPath("$.data.contacts[0].value") { value("h@gosrock.band") }
            }
        }

        @Test
        fun `진행 중 OPEN 은 ONGOING, 종료된 OPEN·정산중·지난공연은 PAST`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "지난$tok")
            val expected = mapOf(
                event(master, hostId, "지난$tok-1", EventStatus.CLOSED, now.minusDays(3)) to "PAST",
                event(master, hostId, "지난$tok-2", EventStatus.CALCULATING, now.minusDays(1)) to "PAST",
                event(master, hostId, "지난$tok-3", EventStatus.OPEN, now.minusMinutes(30)) to "ONGOING",
                event(master, hostId, "지난$tok-4", EventStatus.OPEN, now.minusHours(3)) to "PAST",
            )
            expected.forEach { (id, display) ->
                anonymousGet("/api/v2/events/$id").andExpect {
                    status { isOk() }
                    jsonPath("$.data.displayStatus") { value(display) }
                }
            }
        }

        @Test
        fun `준비중·삭제·없는 공연은 404 - 멤버(마스터)여도`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "숨김$tok")
            val preparing = event(master, hostId, "숨김$tok-1", EventStatus.PREPARING, now.plusDays(1))
            val deleted = event(master, hostId, "숨김$tok-2", EventStatus.DELETED, now.plusDays(1))
            listOf(preparing, deleted, 999_999_999L).forEach { id ->
                anonymousGet("/api/v2/events/$id").andExpect {
                    status { isNotFound() }
                    jsonPath("$.code") { value("Event_404_1") }
                }
                mockMvc.get("/api/v2/events/$id") { with(auth(master)) }.andExpect { status { isNotFound() } }
            }
        }

        @Test
        fun `P-4 섹션 - 등록 공연은 비로그인 조회, 준비중은 비로그인 404 (E-5 그대로)`() {
            val tok = token()
            val master = newUser("마스터")
            val hostId = createHost(master, "섹션$tok")
            val open = event(master, hostId, "섹션$tok-1", EventStatus.OPEN, now.plusDays(1))
            val preparing = event(master, hostId, "섹션$tok-2", EventStatus.PREPARING, now.plusDays(1))
            anonymousGet("/api/v2/events/$open/sections").andExpect { status { isOk() } }
            anonymousGet("/api/v2/events/$preparing/sections").andExpect { status { isNotFound() } }
        }
    }

    // ===== P-5 =====

    @Nested
    @DisplayName("P-5 판매 중 티켓")
    inner class Tickets {

        private fun tickets(eventId: Long): ResultActionsDsl = anonymousGet("/api/v2/events/$eventId/ticket-items")

        private fun saveRaw(item: TicketItem, block: TicketItem.() -> Unit): TicketItem {
            val fresh = ticketItemRepository.findById(item.id!!).get()
            fresh.block()
            return ticketItemRepository.save(fresh)
        }

        @Test
        fun `비로그인 - 판매 중인 유효 티켓만 생성 순, 판매 중단·판매 기간 밖·삭제 제외, 계좌 미노출`() {
            val team = Team()
            val dudoong = createTicket(team.manager, team.eventId, dudoongBody(name = "두둥"))
            val unlimited = createTicket(team.manager, team.eventId, freeBody(name = "무제한", supplyCount = null, overrides = mapOf("isQuantityPublic" to false, "purchaseLimit" to null)))
            val hidden = createTicket(team.manager, team.eventId, freeBody(name = "비공개", supplyCount = 10, overrides = mapOf("isQuantityPublic" to false)))
            val soldOut = createTicket(team.manager, team.eventId, freeBody(name = "매진", supplyCount = 5))
            val suspended = createTicket(team.manager, team.eventId, freeBody(name = "중단"))
            val future = createTicket(team.manager, team.eventId, freeBody(name = "판매전", overrides = mapOf("saleStartAt" to LocalDateTime.now().plusDays(1).f())))
            val ended = createTicket(team.manager, team.eventId, freeBody(name = "판매종료"))
            val deleted = createTicket(team.manager, team.eventId, freeBody(name = "삭제"))
            val yesNo = createOption(team.manager, team.eventId, type = "YES_NO", yesAdditionalPrice = 1000, name = "뒷풀이")
            val subjective = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "입금자명")
            putOptions(team.manager, team.eventId, dudoong, listOf(yesNo, subjective)).andExpect { status { isOk() } }

            suspend(team.manager, team.eventId, suspended).andExpect { status { isOk() } }
            deleteTicket(team.manager, team.eventId, deleted).andExpect { status { isOk() } }
            // 판매 종료가 지난 티켓 (생성 검증을 우회해 직접 기록), 재고 0
            saveRaw(ticketItemRepository.findById(ended).get()) { saleEndAt = LocalDateTime.now().minusMinutes(1) }
            saveRaw(ticketItemRepository.findById(soldOut).get()) { quantity = 0 }
            setEventStatus(team.eventId, EventStatus.OPEN)

            val result = tickets(team.eventId).andExpect { status { isOk() } }
            val raw = result.andReturn().response.getContentAsString(Charsets.UTF_8)
            val data = result.data()
            assertEquals(listOf(dudoong, unlimited, hidden, soldOut), data.map { it.at("/ticketItemId").asLong() })
            // 계좌 미노출
            assertFalse(raw.contains("110-123-456789"))
            assertFalse(raw.contains("신한은행"))
            data.forEach { assertFalse(it.has("account")) }

            val d = data[0]
            assertEquals("두둥", d.at("/name").asText())
            assertEquals("일반 입장", d.at("/description").asText())
            assertEquals(6000, d.at("/price").asInt())
            assertEquals("DUDOONG", d.at("/payType").asText())
            assertTrue(d.at("/approvalRequired").asBoolean())
            assertEquals(100, d.at("/remaining").asInt())
            assertFalse(d.at("/isSoldOut").asBoolean())
            assertTrue(d.at("/isPurchasable").asBoolean())
            assertEquals(4, d.at("/purchaseLimit").asInt())
            assertEquals(
                listOf("$yesNo:뒷풀이:참석하나요?:YES_NO:1000", "$subjective:입금자명:참석하나요?:SUBJECTIVE:null"),
                d.at("/options").map {
                    "${it.at("/optionId").asLong()}:${it.at("/name").asText()}:${it.at("/description").asText()}:${it.at("/type").asText()}:${it.at("/yesAdditionalPrice").let { p -> if (p.isNull) "null" else p.asText() }}"
                },
            )

            val u = data[1]
            assertEquals("FREE", u.at("/payType").asText())
            assertFalse(u.at("/approvalRequired").asBoolean())
            assertTrue(u.at("/remaining").isNull, "무제한은 null")
            assertTrue(u.at("/purchaseLimit").isNull, "제한 없음은 null")
            assertFalse(u.at("/isSoldOut").asBoolean())
            assertTrue(u.at("/isPurchasable").asBoolean())
            assertEquals(0, u.at("/options").size())

            assertTrue(data[2].at("/remaining").isNull, "재고 비공개는 null")
            assertFalse(data[2].at("/isSoldOut").asBoolean())

            assertEquals(0, data[3].at("/remaining").asInt())
            assertTrue(data[3].at("/isSoldOut").asBoolean())
            assertFalse(data[3].at("/isPurchasable").asBoolean(), "매진은 구매 불가")
            assertTrue(data[2].at("/isPurchasable").asBoolean())

            listOf(suspended, future, ended, deleted).forEach { hiddenId -> assertFalse(data.any { it.at("/ticketItemId").asLong() == hiddenId }) }

            // 판매 재개하면 다시 보인다
            resume(team.manager, team.eventId, suspended).andExpect { status { isOk() } }
            assertTrue(tickets(team.eventId).data().any { it.at("/ticketItemId").asLong() == suspended })
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(EventStatus::class, names = ["PREPARING", "DELETED"])
        fun `준비중·삭제 공연의 공개 티켓은 비로그인·멤버 모두 404`(eventStatus: EventStatus) {
            val team = Team()
            createTicket(team.manager, team.eventId, freeBody())
            setEventStatus(team.eventId, eventStatus)

            tickets(team.eventId).andExpect {
                status { isNotFound() }
                jsonPath("$.code") { value("Event_404_1") }
            }
            mockMvc.get("/api/v2/events/${team.eventId}/ticket-items") { with(auth(team.master)) }.andExpect { status { isNotFound() } }
        }

        @Test
        fun `없는 공연의 공개 티켓은 404`() {
            tickets(999_999_999L).andExpect { status { isNotFound() } }
        }

        @Test
        fun `시작 전 등록 공연의 판매 중 티켓은 isPurchasable=true`() {
            val team = Team()
            createTicket(team.manager, team.eventId, freeBody())
            setEventStatus(team.eventId, EventStatus.OPEN)

            tickets(team.eventId).andExpect { jsonPath("$.data[0].isPurchasable") { value(true) } }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(EventStatus::class, names = ["OPEN", "CALCULATING", "CLOSED"])
        fun `시작한 등록·정산중·지난 공연은 티켓 목록이 보이지만 isPurchasable=false`(eventStatus: EventStatus) {
            val team = Team()
            createTicket(team.manager, team.eventId, freeBody())
            val event = eventRepository.findById(team.eventId).get()
            ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = event.getEventName(), startAt = now.minusMinutes(1), runTime = 120))
            eventRepository.save(event)
            setEventStatus(team.eventId, eventStatus)

            tickets(team.eventId).andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].isPurchasable") { value(false) }
            }
        }

        @Test
        fun `기존 PG 티켓(PRICE)은 판매 중이면 목록에 보이지만 v2 주문 경로가 없어 isPurchasable=false`() {
            val team = Team()
            val free = createTicket(team.manager, team.eventId, freeBody())
            val pg = ticketItemRepository.save(
                TicketItem(
                    payType = TicketPayType.PRICE_TICKET, name = "PG", description = "v1 카드결제", price = Money.wons(5000),
                    quantity = 10, supplyCount = 10, purchaseLimit = 2, type = TicketType.FIRST_COME_FIRST_SERVED,
                    isQuantityPublic = true, isSellable = true, eventId = team.eventId,
                ),
            )
            setEventStatus(team.eventId, EventStatus.OPEN)
            val data = tickets(team.eventId).andExpect { status { isOk() } }.data()
            assertEquals(listOf(free, pg.id), data.map { it.at("/ticketItemId").asLong() })
            assertTrue(data[0].at("/isPurchasable").asBoolean())
            assertEquals("PRICE", data[1].at("/payType").asText())
            assertFalse(data[1].at("/isPurchasable").asBoolean())
        }

        @Test
        fun `옵션 N+1 없음 - 티켓·옵션 수와 관계없이 쿼리 수가 같다`() {
            val team = Team()
            val yesNo = createOption(team.manager, team.eventId, type = "YES_NO", yesAdditionalPrice = 1000, name = "뒷풀이")
            val subjective = createOption(team.manager, team.eventId, type = "SUBJECTIVE", yesAdditionalPrice = null, name = "입금자명")
            fun ticketWithOptions(name: String) =
                createTicket(team.manager, team.eventId, dudoongBody(name = name)).also { putOptions(team.manager, team.eventId, it, listOf(yesNo, subjective)).andExpect { status { isOk() } } }
            ticketWithOptions("t1")
            setEventStatus(team.eventId, EventStatus.OPEN)
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            statistics.isStatisticsEnabled = true
            try {
                fun countQueries(expectedTickets: Int): Long {
                    statistics.clear()
                    val data = tickets(team.eventId).andExpect { status { isOk() } }.data()
                    assertEquals(expectedTickets, data.size())
                    assertTrue(data.all { it.at("/options").size() == 2 && it.at("/options/0/yesAdditionalPrice").asInt() == 1000 })
                    return statistics.prepareStatementCount
                }
                val one = countQueries(1)
                listOf("t2", "t3", "t4").forEach { ticketWithOptions(it) }
                val four = countQueries(4)
                assertEquals(one, four, "티켓 1개: $one, 4개: $four")
                // 공연 1 + 티켓·옵션 그룹 fetch join 1 + 옵션 선택지 batch 1 (fetch join 전: 1개 6, 4개 9)
                assertEquals(3L, four)
            } finally {
                statistics.isStatisticsEnabled = false
            }
        }

        @Test
        fun `T-1 관리 목록(인증)과 경로가 겹치지 않는다 - manage 는 여전히 비로그인 401`() {
            val team = Team()
            mockMvc.get("/api/v2/events/${team.eventId}/ticket-items/manage").andExpect { status { isUnauthorized() } }
            // POST 생성은 여전히 인증 필요
            mockMvc.post("/api/v2/events") {
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("hostId" to team.hostId, "name" to "x"))
            }.andExpect { status { isUnauthorized() } }
        }
    }
}
