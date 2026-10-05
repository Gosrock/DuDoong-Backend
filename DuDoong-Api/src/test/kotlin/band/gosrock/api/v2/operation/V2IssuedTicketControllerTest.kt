package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.event.repository.EventRepository
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInDomainService
import band.gosrock.domain.domains.issuedTicket.service.v2.V2CheckInResult
import java.util.Collections
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

/** v2 공연 운영 - 발급 티켓·QR 체크인 통합 테스트 (#712): I-1 ~ I-3, Q-1, Q-2, Q-4, Q-5 + 동시 스캔 + v1 호환 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공연 운영 - 발급 티켓·체크인")
class V2IssuedTicketControllerTest : V2OperationTestSupport() {

    @Autowired private lateinit var v2CheckInDomainService: V2CheckInDomainService

    @Autowired private lateinit var eventRepositoryForToken: EventRepository

    private fun result(requester: band.gosrock.domain.domains.user.domain.User, eventId: Long, ticketUuid: String) =
        checkIn(requester, eventId, ticketUuid).andExpect { status { isOk() } }.data()

    @Nested
    @DisplayName("I-1 목록 / I-2 상세 / I-3 엑셀")
    inner class Tickets {

        @Test
        fun `유효 티켓만, 입장 필터·건수·검색, 상세 옵션 응답·연락처, 다른 공연 티켓 404`() {
            val shop = Shop()
            val alice = newBuyer("앨리스", "010-1000-2000")
            val bob = newBuyer("밥")
            val aliceOrder = shop.approved(alice, quantity = 2)
            val bobOrder = shop.approved(bob)
            val canceledOrder = shop.approved(newBuyer("취소"))
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$canceledOrder/cancel").andExpect { status { isOk() } }
            val aliceTickets = shop.ticketUuids(aliceOrder)
            result(shop.team.guest, shop.eventId, aliceTickets[0])

            val all = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets").andExpect { status { isOk() } }.data()
            assertEquals(3, all.at("/counts/issuedCount").asLong())
            assertEquals(1, all.at("/counts/enteredCount").asLong())
            assertEquals(2, all.at("/counts/notEnteredCount").asLong())
            assertEquals(3, all.at("/tickets/totalElements").asLong())
            val first = all.at("/tickets/content").first { it.at("/ticketUuid").asText() == aliceTickets[0] }
            assertEquals("DONE", first.at("/entrance").asText())
            assertNotNull(first.at("/enteredAt").textValue())
            assertEquals("앨리스", first.at("/buyerName").asText())
            assertEquals("DUDOONG", first.at("/payType").asText())
            assertEquals("일반", first.at("/ticketName").asText())
            assertEquals(orderRepository.findByOrderUuid(aliceOrder).get().orderNo, first.at("/orderNo").asText())
            assertTrue(first.at("/issuedTicketNo").asText().startsWith("T"))

            val done = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets", mapOf("entrance" to "DONE")).andExpect { status { isOk() } }.data()
            assertEquals(listOf(aliceTickets[0]), done.at("/tickets/content").map { it.at("/ticketUuid").asText() })
            assertEquals(3, done.at("/counts/issuedCount").asLong())
            val before = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets", mapOf("entrance" to "BEFORE")).andExpect { status { isOk() } }.data()
            assertEquals(setOf(aliceTickets[1], shop.ticketUuids(bobOrder)[0]), before.at("/tickets/content").map { it.at("/ticketUuid").asText() }.toSet())
            val byName = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets", mapOf("keyword" to "밥")).andExpect { status { isOk() } }.data()
            assertEquals(1, byName.at("/counts/issuedCount").asLong())
            val byPhone = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets", mapOf("searchType" to "PHONE", "keyword" to "1000-2000")).andExpect { status { isOk() } }.data()
            assertEquals(2, byPhone.at("/tickets/totalElements").asLong())

            v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/${aliceTickets[1]}").andExpect {
                status { isOk() }
                jsonPath("$.data.ticket.entrance") { value("BEFORE") }
                jsonPath("$.data.buyerPhone") { value("010-1000-2000") }
                jsonPath("$.data.optionAnswers.length()") { value(2) }
            }
            // 취소 티켓도 상세는 보인다
            v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/${shop.ticketUuids(canceledOrder)[0]}").andExpect {
                status { isOk() }
                jsonPath("$.data.ticket.entrance") { value("CANCELED") }
            }
            val other = Shop("남의호스트")
            val otherTicket = shop.ticketUuids(other.approved(newBuyer()))[0]
            assertEquals("IssuedTicket_404_1", v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/$otherTicket").andExpect { status { isNotFound() } }.code())
            v2Get(shop.team.guest, "/events/${other.eventId}/issued-tickets/$otherTicket").andExpect { status { isForbidden() } }
            v2Get(shop.team.outsider, "/events/${shop.eventId}/issued-tickets").andExpect { status { isForbidden() } }
            v2Get(null, "/events/${shop.eventId}/issued-tickets").andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `엑셀 - 기본 헤더 + 옵션 컬럼, 행 수는 필터와 같다`() {
            val shop = Shop()
            // 1인 4장 제한에 한 주문 3장은 H2 에서만 승인 실패한다 (V2OperationTestSupport.Shop 주석). 2장 + 1장으로 3행을 만든다
            val order = shop.approved(newBuyer("엑셀", "010-7777-8888"), quantity = 2)
            shop.approved(newBuyer("=엑셀2", "010-7777-8888"))
            result(shop.team.guest, shop.eventId, shop.ticketUuids(order)[0])
            val sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect {
                status { isOk() }
                header { string("Content-Disposition", "attachment; filename=\"issued-tickets-${shop.eventId}.xlsx\"") }
            }.sheet()
            assertEquals(
                listOf("티켓번호", "티켓 종류", "티켓 이름", "주문자", "연락처", "주문번호", "발급일시", "입장", "체크인 시각", "뒷풀이", "입금자명"),
                sheet.headers(),
            )
            assertEquals(3, sheet.lastRowNum)
            assertEquals(setOf("예"), sheet.column("뒷풀이").toSet())
            assertEquals(setOf("홍길동"), sheet.column("입금자명").toSet())
            assertEquals(setOf("010-7777-8888"), sheet.column("연락처").toSet())
            // 이메일 컬럼 없음, 수식으로 해석될 이름은 작은따옴표
            assertTrue("이메일" !in sheet.headers())
            assertTrue("'=엑셀2" in sheet.column("주문자"))
            assertEquals(listOf("입장 완료", "입장 전", "입장 전").sorted(), sheet.column("입장").sorted())
            val done = v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export", mapOf("entrance" to "DONE")).andExpect { status { isOk() } }.sheet()
            assertEquals(1, done.lastRowNum)
            for (who in listOf(shop.team.guest, shop.team.manager, shop.team.master, superAdmin())) {
                v2Get(who, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isOk() } }
            }
            v2Get(shop.team.outsider, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isForbidden() } }
            v2Get(null, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isUnauthorized() } }
        }
    }

    @Nested
    @DisplayName("Q-2 호스트 스캔 / Q-1 통계")
    inner class HostScan {

        @Test
        fun `결과 코드 4종 - ENTERED, ALREADY_ENTERED, OTHER_EVENT(다른 공연·없는 uuid, 티켓 정보 없음), CANCELED`() {
            val shop = Shop()
            val order = shop.approved(newBuyer("관객"))
            val ticket = shop.ticketUuids(order)[0]
            result(shop.team.guest, shop.eventId, ticket).let {
                assertEquals("ENTERED", it.at("/result").asText())
                assertEquals(ticket, it.at("/ticket/ticketUuid").asText())
                assertEquals("관객", it.at("/ticket/buyerName").asText())
                assertEquals("DONE", it.at("/ticket/entrance").asText())
                assertNotNull(it.at("/ticket/enteredAt").textValue())
            }
            result(shop.team.manager, shop.eventId, ticket).let {
                assertEquals("ALREADY_ENTERED", it.at("/result").asText())
                assertEquals(ticket, it.at("/ticket/ticketUuid").asText())
            }
            val other = Shop("남의호스트")
            val otherTicket = other.ticketUuids(other.approved(newBuyer()))[0]
            for (uuid in listOf(otherTicket, UUID.randomUUID().toString(), "garbage")) {
                result(shop.team.guest, shop.eventId, uuid).let {
                    assertEquals("OTHER_EVENT", it.at("/result").asText())
                    assertTrue(it.at("/ticket").isNull)
                }
            }
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(otherTicket).get().issuedTicketStatus)

            val canceled = shop.approved(newBuyer())
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$canceled/cancel").andExpect { status { isOk() } }
            result(shop.team.guest, shop.eventId, shop.ticketUuids(canceled)[0]).let {
                assertEquals("CANCELED", it.at("/result").asText())
                assertEquals("CANCELED", it.at("/ticket/entrance").asText())
            }

            v2Get(shop.team.guest, "/events/${shop.eventId}/check-ins/stats").andExpect {
                status { isOk() }
                jsonPath("$.data.issuedCount") { value(1) }
                jsonPath("$.data.enteredCount") { value(1) }
                jsonPath("$.data.notEnteredCount") { value(0) }
                jsonPath("$.data.entranceRate") { value(100.0) }
            }
        }

        @Test
        fun `권한 - 일반 멤버 가능(DEC-009), 외부인·다른 호스트 403, 비로그인 401, 빈 uuid 400, SUPER_ADMIN 가능`() {
            val shop = Shop()
            val tickets = shop.ticketUuids(shop.approved(newBuyer(), quantity = 2))
            checkIn(shop.team.outsider, shop.eventId, tickets[0]).andExpect { status { isForbidden() } }
            v2Post(null, "/events/${shop.eventId}/check-ins", mapOf("ticketUuid" to tickets[0])).andExpect { status { isUnauthorized() } }
            checkIn(shop.team.guest, shop.eventId, "").andExpect { status { isBadRequest() } }
            v2Get(shop.team.outsider, "/events/${shop.eventId}/check-ins/stats").andExpect { status { isForbidden() } }
            val other = Shop("남의호스트")
            checkIn(other.team.master, shop.eventId, tickets[0]).andExpect { status { isForbidden() } }
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(tickets[0]).get().issuedTicketStatus)
            assertEquals("ENTERED", result(shop.team.guest, shop.eventId, tickets[0]).at("/result").asText())
            assertEquals("ENTERED", result(superAdmin(), shop.eventId, tickets[1]).at("/result").asText())
        }

        @Test
        fun `공연 상태 - OPEN·CALCULATING(지각 입장)만 입장 처리, 준비중·지난공연 Event_400_27, 삭제된 공연 404`() {
            val shop = Shop()
            val tickets = shop.ticketUuids(shop.approved(newBuyer(), quantity = 2))
            for (status in listOf(EventStatus.PREPARING, EventStatus.CLOSED)) {
                setEventStatus(shop.eventId, status)
                assertEquals("Event_400_27", checkIn(shop.team.guest, shop.eventId, tickets[0]).andExpect { status { isBadRequest() } }.code())
            }
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(tickets[0]).get().issuedTicketStatus)
            setEventStatus(shop.eventId, EventStatus.CALCULATING)
            assertEquals("ENTERED", result(shop.team.guest, shop.eventId, tickets[0]).at("/result").asText())
            setEventStatus(shop.eventId, EventStatus.DELETED)
            checkIn(superAdmin(), shop.eventId, tickets[1]).andExpect { status { isNotFound() } }
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(tickets[1]).get().issuedTicketStatus)
        }

        @Test
        fun `동시 스캔 - 같은 티켓 10번 동시에 스캔하면 ENTERED 1번, 나머지 ALREADY_ENTERED`() {
            val shop = Shop()
            val ticket = shop.ticketUuids(shop.approved(newBuyer()))[0]
            val threads = 10
            val pool = Executors.newFixedThreadPool(threads)
            val ready = CountDownLatch(threads)
            val start = CountDownLatch(1)
            val results = Collections.synchronizedList(mutableListOf<V2CheckInResult>())
            repeat(threads) {
                pool.submit {
                    ready.countDown()
                    start.await()
                    results.add(v2CheckInDomainService.checkIn(shop.eventId, ticket).result)
                }
            }
            ready.await()
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
            assertEquals(threads, results.size, "모든 요청이 결과를 받아야 한다 (락 대기 실패 없음)")
            assertEquals(1, results.count { it == V2CheckInResult.ENTERED })
            assertEquals(threads - 1, results.count { it == V2CheckInResult.ALREADY_ENTERED })
        }
    }

    @Nested
    @DisplayName("Q-4 셀프 체크인 QR")
    inner class Qr {

        @Test
        fun `공연별 고정 토큰 - 최초 조회 시 생성, 다시 조회해도 같음, 공연마다 다름, 일반 멤버 가능, 외부인 403`() {
            val shop = Shop()
            assertEquals(null, eventRepositoryForToken.findCheckInTokenById(shop.eventId))
            val data = v2Get(shop.team.guest, "/events/${shop.eventId}/check-in-qr").andExpect { status { isOk() } }.data()
            val token = data.at("/token").asText()
            assertEquals(43, token.length)
            assertEquals("/check-in?token=$token", data.at("/qrPath").asText())
            assertEquals(token, qrToken(shop.team.master, shop.eventId))
            assertEquals(token, eventRepositoryForToken.findCheckInTokenById(shop.eventId))
            val other = Shop("남의호스트")
            assertTrue(qrToken(other.team.master, other.eventId) != token)
            v2Get(shop.team.outsider, "/events/${shop.eventId}/check-in-qr").andExpect { status { isForbidden() } }
            v2Get(superAdmin(), "/events/999999999/check-in-qr").andExpect { status { isNotFound() } }
        }

        @Test
        fun `동시 최초 조회 - 모두 같은 토큰을 받는다`() {
            val shop = Shop()
            val threads = 8
            val pool = Executors.newFixedThreadPool(threads)
            val start = CountDownLatch(1)
            val tokens = Collections.synchronizedList(mutableListOf<String>())
            repeat(threads) {
                pool.submit {
                    start.await()
                    tokens.add(v2CheckInDomainService.getOrCreateCheckInToken(shop.eventId))
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
            assertEquals(threads, tokens.size)
            assertEquals(1, tokens.toSet().size)
            assertEquals(tokens[0], eventRepositoryForToken.findCheckInTokenById(shop.eventId))
        }
    }

    @Nested
    @DisplayName("Q-5 관객 셀프 체크인")
    inner class Self {

        @Test
        fun `1장이면 바로 입장, 다시 하면 ALREADY_ENTERED`() {
            val shop = Shop()
            val buyer = newBuyer("셀프")
            val ticket = shop.ticketUuids(shop.approved(buyer))[0]
            val token = qrToken(shop.team.guest, shop.eventId)
            selfCheckIn(buyer, token).andExpect {
                status { isOk() }
                jsonPath("$.data.result") { value("ENTERED") }
                jsonPath("$.data.ticket.ticketUuid") { value(ticket) }
                jsonPath("$.data.candidates.length()") { value(0) }
            }
            selfCheckIn(buyer, token).andExpect {
                status { isOk() }
                jsonPath("$.data.result") { value("ALREADY_ENTERED") }
                jsonPath("$.data.ticket.ticketUuid") { value(ticket) }
            }
            selfCheckIn(buyer, token, ticket).andExpect { jsonPath("$.data.result") { value("ALREADY_ENTERED") } }
        }

        @Test
        fun `여러 장이면 SELECT_TICKET + 입장 전 후보, ticketUuid 로 하나씩 입장`() {
            val shop = Shop()
            val buyer = newBuyer("다장")
            val tickets = shop.ticketUuids(shop.approved(buyer, quantity = 2))
            val token = qrToken(shop.team.guest, shop.eventId)
            val select = selfCheckIn(buyer, token).andExpect { status { isOk() } }.data()
            assertEquals("SELECT_TICKET", select.at("/result").asText())
            assertTrue(select.at("/ticket").isNull)
            assertEquals(tickets, select.at("/candidates").map { it.at("/ticketUuid").asText() })
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(tickets[0]).get().issuedTicketStatus)

            selfCheckIn(buyer, token, tickets[1]).andExpect { jsonPath("$.data.result") { value("ENTERED") } }
            // 남은 1장은 지정 없이도 바로 입장
            selfCheckIn(buyer, token).andExpect {
                jsonPath("$.data.result") { value("ENTERED") }
                jsonPath("$.data.ticket.ticketUuid") { value(tickets[0]) }
            }
            selfCheckIn(buyer, token).andExpect { jsonPath("$.data.result") { value("ALREADY_ENTERED") } }
        }

        @Test
        fun `실패 - 남의 티켓 400, 다른 공연 토큰·티켓 없음 OTHER_EVENT, 취소 티켓 CANCELED, 잘못된 토큰 400, OPEN 아님 400, 비로그인 401`() {
            val shop = Shop()
            val buyer = newBuyer("주인")
            val stranger = newBuyer("남")
            val ticket = shop.ticketUuids(shop.approved(buyer))[0]
            val token = qrToken(shop.team.guest, shop.eventId)

            assertEquals("IssuedTicket_400_1", selfCheckIn(stranger, token, ticket).andExpect { status { isBadRequest() } }.code())
            selfCheckIn(stranger, token).andExpect {
                status { isOk() }
                jsonPath("$.data.result") { value("OTHER_EVENT") }
            }
            val other = Shop("남의호스트")
            val otherToken = qrToken(other.team.master, other.eventId)
            selfCheckIn(buyer, otherToken, ticket).andExpect { jsonPath("$.data.result") { value("OTHER_EVENT") } }
            selfCheckIn(buyer, otherToken).andExpect { jsonPath("$.data.result") { value("OTHER_EVENT") } }
            assertEquals("Event_400_26", selfCheckIn(buyer, "wrong-token").andExpect { status { isBadRequest() } }.code())
            selfCheckIn(null, token).andExpect { status { isUnauthorized() } }
            selfCheckIn(buyer, "").andExpect { status { isBadRequest() } }
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(ticket).get().issuedTicketStatus)

            val canceledBuyer = newBuyer("취소관객")
            val canceled = shop.approved(canceledBuyer)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$canceled/cancel").andExpect { status { isOk() } }
            selfCheckIn(canceledBuyer, token).andExpect { jsonPath("$.data.result") { value("CANCELED") } }

            // 셀프 체크인은 OPEN 만 (호스트 스캔과 달리 정산중 불가)
            setEventStatus(shop.eventId, EventStatus.CALCULATING)
            assertEquals("Event_400_5", selfCheckIn(buyer, token).andExpect { status { isBadRequest() } }.code())
            assertEquals(IssuedTicketStatus.ENTRANCE_INCOMPLETE, issuedTicketRepository.findByUuid(ticket).get().issuedTicketStatus)
            // 삭제된 공연의 토큰은 잘못된 토큰과 같다 (@Where 로 조회 안 됨), QR 조회도 404
            setEventStatus(shop.eventId, EventStatus.DELETED)
            assertEquals("Event_400_26", selfCheckIn(buyer, token).andExpect { status { isBadRequest() } }.code())
            v2Get(superAdmin(), "/events/${shop.eventId}/check-in-qr").andExpect { status { isNotFound() } }
        }
    }

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 입장 → v1 발급 티켓 목록 입장 완료, v1 입장 API 는 이미 입장 400, v1 입장 → v2 ALREADY_ENTERED`() {
            val shop = Shop()
            val tickets = shop.ticketUuids(shop.approved(newBuyer(), quantity = 2))
            result(shop.team.guest, shop.eventId, tickets[0])
            mockMvc.get("/api/v1/events/${shop.eventId}/issuedTickets") { with(auth(shop.team.master)) }.andExpect {
                status { isOk() }
            }.data().at("/content").associateBy { it.at("/uuid").asText() }.let {
                assertEquals("입장 완료", it.getValue(tickets[0]).at("/issuedTicketStatus").asText())
                assertEquals("입장 전", it.getValue(tickets[1]).at("/issuedTicketStatus").asText())
            }
            mockMvc.patch("/api/v1/events/${shop.eventId}/issuedTickets/${tickets[0]}") { with(auth(shop.team.master)) }.andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value("IssuedTicket_400_5") }
            }
            mockMvc.patch("/api/v1/events/${shop.eventId}/issuedTickets/${tickets[1]}") { with(auth(shop.team.master)) }.andExpect { status { isOk() } }
            assertEquals("ALREADY_ENTERED", result(shop.team.guest, shop.eventId, tickets[1]).at("/result").asText())
            // v1 입장 API 권한(매니저 이상)은 그대로
            mockMvc.patch("/api/v1/events/${shop.eventId}/issuedTickets/${tickets[1]}") { with(auth(shop.team.guest)) }.andExpect { status { isBadRequest() } }
        }
    }
}
