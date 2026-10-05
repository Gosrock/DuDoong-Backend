package band.gosrock.api.v2

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.gift.V2GiftTestSupport
import band.gosrock.api.v2.operation.usecase.V2ExcelHeaders
import band.gosrock.domain.domains.event.domain.EventBasic
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManagerFactory
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.LocalDateTime
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch

/**
 * 전수조사 후속 (#740): 상세주소, 호스트 발급 티켓의 선물 상태·주문자/소유자, dDay(E-3·D-1·H-14), Swagger 문구.
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 전수조사 후속 (#740)")
class V2AuditFollowupTest : V2GiftTestSupport() {

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private val place = mapOf("name" to "롤링홀", "address" to "서울 마포구 어울마당로 35", "latitude" to 37.548369, "longitude" to 126.920036)

    private fun patchBasic(requester: User, eventId: Long, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.patch("/api/v2/events/$eventId/basic") {
            with(auth(requester))
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    private fun eventManage(requester: User, eventId: Long): JsonNode =
        v2Get(requester, "/events/$eventId/manage").andExpect { status { isOk() } }.data()

    private fun storedDetail(eventId: Long): String? = eventRepository.findById(eventId).get().eventPlace?.placeDetailAddress

    private fun setSchedule(eventId: Long, status: EventStatus, startAt: LocalDateTime, runTime: Long = 120) {
        val event = eventRepository.findById(eventId).get()
        ReflectionTestUtils.setField(event, "eventBasic", EventBasic(name = event.getEventName(), startAt = startAt, runTime = runTime))
        ReflectionTestUtils.setField(event, "status", status)
        eventRepository.save(event)
    }

    // ===== 1. 상세주소 =====

    @Nested
    @DisplayName("상세주소 (E-4 → E-3·E-1·P-3·H-14)")
    inner class PlaceDetail {

        @Test
        fun `E-4 로 저장하면 장소를 응답하는 모든 v2 API 에 나온다`() {
            val team = Team()
            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "지하 1층"))).andExpect {
                status { isOk() }
                jsonPath("$.data.place.detailAddress") { value("지하 1층") }
            }
            assertEquals("지하 1층", eventManage(team.guest, team.eventId).at("/place/detailAddress").asText())
            val myEvent = v2Get(team.master, "/me/events", mapOf("size" to "50")).data().at("/content")
                .first { it.at("/eventId").asLong() == team.eventId }
            assertEquals("지하 1층", myEvent.at("/placeDetailAddress").asText())

            setEventStatus(team.eventId, EventStatus.OPEN)
            assertEquals("지하 1층", mockMvc.get("/api/v2/events/${team.eventId}").andExpect { status { isOk() } }.data().at("/place/detailAddress").asText())
            val hostEvent = mockMvc.get("/api/v2/hosts/${team.hostId}/events").data().at("/content")
                .first { it.at("/eventId").asLong() == team.eventId }
            assertEquals("지하 1층", hostEvent.at("/place/detailAddress").asText())
        }

        @Test
        fun `정리 규칙 — 제어 문자 제거·앞뒤 공백 제거, 비면 null, 빼면 지워짐(장소 통째 교체), 255자 초과는 400`() {
            val team = Team()
            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "  B1\n\t층\u0000 "))).andExpect { status { isOk() } }
            assertEquals("B1층", storedDetail(team.eventId))

            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to " \n "))).andExpect {
                status { isOk() }
                jsonPath("$.data.place.detailAddress") { value(null as Any?) }
            }
            assertNull(storedDetail(team.eventId))

            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "2층"))).andExpect { status { isOk() } }
            patchBasic(team.manager, team.eventId, mapOf("place" to place)).andExpect { status { isOk() } }
            assertNull(storedDetail(team.eventId))

            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "가".repeat(256)))).andExpect { status { isBadRequest() } }
            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "가".repeat(255)))).andExpect { status { isOk() } }
            // 장소를 안 보내면 상세주소도 그대로
            patchBasic(team.manager, team.eventId, mapOf("name" to "이름만")).andExpect { status { isOk() } }
            assertEquals("가".repeat(255), storedDetail(team.eventId))
        }

        @Test
        fun `v1 기본 정보 수정 — 주소가 같으면 상세주소 유지, 주소가 바뀌면 지움, v1 응답에는 상세주소 필드가 없다`() {
            val team = Team()
            patchBasic(team.manager, team.eventId, mapOf("place" to place + ("detailAddress" to "지하 1층"))).andExpect { status { isOk() } }
            fun v1Basic(address: String) = mockMvc.patch("/api/v1/events/${team.eventId}/basic") {
                with(auth(team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(
                    mapOf(
                        "name" to "v1수정", "startAt" to eventStart.f(), "runTime" to 120,
                        "placeName" to "롤링홀", "placeAddress" to address, "latitude" to 37.5, "longitude" to 126.9,
                    ),
                )
            }.andExpect { status { isOk() } }
            v1Basic(place["address"] as String)
            assertEquals("지하 1층", storedDetail(team.eventId))
            v1Basic("서울 마포구 다른 주소 1")
            assertNull(storedDetail(team.eventId))

            val v1Detail = mockMvc.get("/api/v1/events/${team.eventId}") { with(auth(team.master)) }.andExpect { status { isOk() } }.body().toString()
            assertFalse(v1Detail.contains("etailAddress"), v1Detail)
        }
    }

    // ===== 2. 호스트 발급 티켓 선물 상태 =====

    @Nested
    @DisplayName("호스트 발급 티켓 (I-1·I-2·I-3) 선물 상태·주문자/소유자")
    inner class GiftOnHostTickets {

        private fun list(host: User, eventId: Long): JsonNode =
            v2Get(host, "/events/$eventId/issued-tickets", mapOf("size" to "100")).andExpect { status { isOk() } }.data().at("/tickets/content")

        private fun row(host: User, eventId: Long, ticketNo: String): JsonNode =
            list(host, eventId).first { it.at("/issuedTicketNo").asText() == ticketNo }

        private fun detail(host: User, eventId: Long, uuid: String): JsonNode =
            v2Get(host, "/events/$eventId/issued-tickets/$uuid").andExpect { status { isOk() } }.data()

        private fun sheet(host: User, eventId: Long): Sheet {
            val bytes = v2Get(host, "/events/$eventId/issued-tickets/export").andExpect { status { isOk() } }.andReturn().response.contentAsByteArray
            return XSSFWorkbook(ByteArrayInputStream(bytes)).getSheetAt(0)
        }

        private fun Sheet.rowOf(ticketNo: String): Map<String, String> {
            val headers = getRow(0).map { it.stringCellValue }
            val r = (1..lastRowNum).map { getRow(it) }.first { it.getCell(0).stringCellValue == ticketNo }
            return headers.mapIndexed { i, h -> h to (r.getCell(i)?.toString() ?: "") }.toMap()
        }

        @Test
        fun `선물 없음 → 대기(PENDING) → 수락(ACCEPTED, 소유자 = 받은 사람) → 반환(NONE) — 주문자는 계속 보낸 사람`() {
            val shop = Shop()
            val sender = newBuyer("보낸이", "010-1111-2222")
            val receiver = newBuyer("받는이", "010-3333-4444")
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val ticketNo = ticketByUuid(uuids[0]).issuedTicketNo!!
            val host = shop.team.guest

            row(host, shop.eventId, ticketNo).let {
                assertEquals("NONE", it.at("/giftState").asText())
                assertEquals("보낸이", it.at("/buyerName").asText())
                assertEquals("보낸이", it.at("/ownerName").asText())
            }

            val created = giftOk(sender, uuids[0])
            row(host, shop.eventId, ticketNo).let {
                assertEquals("PENDING", it.at("/giftState").asText())
                assertEquals("보낸이", it.at("/ownerName").asText())
            }
            // 같은 주문의 다른 티켓은 영향 없음
            assertEquals("NONE", row(host, shop.eventId, ticketByUuid(uuids[1]).issuedTicketNo!!).at("/giftState").asText())

            val newUuid = accept(receiver, created.at("/giftToken").asText()).andExpect { status { isOk() } }.data().at("/ticketUuid").asText()
            row(host, shop.eventId, ticketNo).let {
                assertEquals("ACCEPTED", it.at("/giftState").asText())
                assertEquals("보낸이", it.at("/buyerName").asText())
                assertEquals("받는이", it.at("/ownerName").asText())
            }
            detail(host, shop.eventId, newUuid).let {
                assertEquals("ACCEPTED", it.at("/ticket/giftState").asText())
                assertEquals("010-1111-2222", it.at("/buyerPhone").asText())
                assertEquals("010-3333-4444", it.at("/ownerPhone").asText())
            }
            sheet(host, shop.eventId).rowOf(ticketNo).let {
                assertEquals("보낸이", it["주문자"])
                assertEquals("010-1111-2222", it["연락처"])
                assertEquals("받는이", it["소유자"])
                assertEquals("010-3333-4444", it["소유자 연락처"])
                assertEquals("선물 완료", it["선물"])
            }

            returnTicket(receiver, newUuid).andExpect { status { isOk() } }
            row(host, shop.eventId, ticketNo).let {
                assertEquals("NONE", it.at("/giftState").asText())
                assertEquals("보낸이", it.at("/ownerName").asText())
            }
        }

        @Test
        fun `엑셀 — 선물 대기는 '선물 대기', 선물 없음은 빈 칸, 헤더는 기본 열 그대로`() {
            val shop = Shop()
            val sender = newBuyer("엑셀보낸이")
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            giftOk(sender, uuids[0])
            val sheet = sheet(shop.team.guest, shop.eventId)
            assertEquals(V2ExcelHeaders.ISSUED_TICKET, sheet.getRow(0).map { it.stringCellValue }.take(V2ExcelHeaders.ISSUED_TICKET.size))
            assertEquals("선물 대기", sheet.rowOf(ticketByUuid(uuids[0]).issuedTicketNo!!)["선물"])
            assertEquals("", sheet.rowOf(ticketByUuid(uuids[1]).issuedTicketNo!!)["선물"])
            assertEquals("엑셀보낸이", sheet.rowOf(ticketByUuid(uuids[0]).issuedTicketNo!!)["소유자"])
        }

        @Test
        fun `거절·회수된 선물은 NONE`() {
            val shop = Shop()
            val sender = newBuyer("거절보낸이")
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val rejected = giftOk(sender, uuids[0])
            reject(newBuyer("거절자"), rejected.at("/giftToken").asText()).andExpect { status { isOk() } }
            val canceled = giftOk(sender, uuids[1])
            cancelGift(sender, canceled.at("/giftId").asLong()).andExpect { status { isOk() } }
            uuids.forEach { assertEquals("NONE", row(shop.team.guest, shop.eventId, ticketByUuid(it).issuedTicketNo!!).at("/giftState").asText()) }
        }

        @Test
        fun `체크인 Q-2·Q-5 응답도 buyerName = 주문자, ownerName = 현재 소유자`() {
            val shop = Shop()
            val sender = newBuyer("체크인보낸이")
            val receiver = newBuyer("체크인받는이")
            val (_, uuids) = approvedOrder(shop, sender, quantity = 2)
            val (_, newUuid) = giveAndAccept(sender, receiver, uuids[0])

            checkIn(shop.team.guest, shop.eventId, newUuid).andExpect { status { isOk() } }.data().at("/ticket").let {
                assertEquals("체크인보낸이", it.at("/buyerName").asText())
                assertEquals("체크인받는이", it.at("/ownerName").asText())
            }
            // 선물 없는 티켓은 같다 — 셀프 체크인(Q-5)
            val token = qrToken(shop.team.master, shop.eventId)
            selfCheckIn(sender, token, uuids[1]).andExpect { status { isOk() } }.data().at("/ticket").let {
                assertEquals("체크인보낸이", it.at("/buyerName").asText())
                assertEquals("체크인보낸이", it.at("/ownerName").asText())
            }
            // 받은 사람의 셀프 체크인: 이미 입장한 티켓 → 요약에 소유자·주문자
            selfCheckIn(receiver, token, newUuid).andExpect { status { isOk() } }.data().let {
                assertEquals("ALREADY_ENTERED", it.at("/result").asText())
                assertEquals("체크인받는이", it.at("/ticket/ownerName").asText())
                assertEquals("체크인보낸이", it.at("/ticket/buyerName").asText())
            }
        }

        @Test
        fun `N+1 없음 — I-3 엑셀도 티켓·선물 수와 관계없이 쿼리 수가 같다`() {
            fun exportQueries(shop: Shop): Long {
                val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
                statistics.isStatisticsEnabled = true
                try {
                    statistics.clear()
                    v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets/export").andExpect { status { isOk() } }
                    return statistics.prepareStatementCount
                } finally {
                    statistics.isStatisticsEnabled = false
                }
            }
            val small = Shop().also { approvedOrder(it, newBuyer("엑셀N1소"), quantity = 1) }
            val large = Shop().also { shop ->
                val buyers = (1..3).map { newBuyer("엑셀N1대$it") }
                val tickets = buyers.flatMap { b -> approvedOrder(shop, b, quantity = 2).second.map { b to it } }
                giftOk(tickets[0].first, tickets[0].second)
                giveAndAccept(tickets[2].first, newBuyer("엑셀N1받는이"), tickets[2].second)
            }
            assertEquals(exportQueries(small), exportQueries(large))
        }

        @Test
        fun `N+1 없음 — 선물이 섞인 티켓 수와 관계없이 I-1 쿼리 수가 같다`() {
            val shop = Shop()
            val buyers = (1..3).map { newBuyer("N1구매$it") }
            val tickets = buyers.flatMap { approvedOrder(shop, it, quantity = 2).second.map { uuid -> it to uuid } }
            // 보낸 사람 3명, 대기 2 + 수락 1
            giftOk(tickets[0].first, tickets[0].second)
            giftOk(tickets[2].first, tickets[2].second)
            giveAndAccept(tickets[4].first, newBuyer("N1받는이"), tickets[4].second)
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            statistics.isStatisticsEnabled = true
            try {
                fun count(size: Int): Long {
                    statistics.clear()
                    v2Get(shop.team.guest, "/events/${shop.eventId}/issued-tickets", mapOf("size" to size.toString())).andExpect { status { isOk() } }
                    return statistics.prepareStatementCount
                }
                val one = count(1)
                val all = count(100)
                // 한 페이지에 다 들어오면 건수 쿼리 1개를 생략한다
                assertEquals(one - 1, all, "size=1: $one, size=100: $all")
            } finally {
                statistics.isStatisticsEnabled = false
            }
        }
    }

    // ===== 3. dDay =====

    @Nested
    @DisplayName("dDay (E-3·D-1·H-14)")
    inner class DDay {

        @Test
        fun `UPCOMING 이면 공연일까지 남은 날짜 수(당일 0), 진행 중·준비중·지난 공연은 null — 세 API 가 같다`() {
            val team = Team()
            fun dDays(): List<JsonNode> = listOf(
                eventManage(team.master, team.eventId).at("/dDay"),
                v2Get(team.master, "/events/${team.eventId}/dashboard").andExpect { status { isOk() } }.data().at("/dDay"),
                mockMvc.get("/api/v2/hosts/${team.hostId}/events") { with(auth(team.master)) }.data().at("/content")
                    .first { it.at("/eventId").asLong() == team.eventId }.at("/dDay"),
            )
            // 준비중 → null
            assertTrue(dDays().all { it.isNull }, dDays().toString())

            val start = LocalDate.now().plusDays(5).atTime(18, 0)
            setSchedule(team.eventId, EventStatus.OPEN, start)
            assertEquals(listOf(5L, 5L, 5L), dDays().map { it.asLong() })
            assertEquals("UPCOMING", v2Get(team.master, "/events/${team.eventId}/dashboard").data().at("/displayStatus").asText())

            // 오늘 늦게 시작 → 0
            val laterToday = LocalDateTime.now().plusMinutes(5)
            if (laterToday.toLocalDate() == LocalDate.now()) {
                setSchedule(team.eventId, EventStatus.OPEN, laterToday)
                assertEquals(listOf(0L, 0L, 0L), dDays().map { it.asLong() })
            }

            setSchedule(team.eventId, EventStatus.OPEN, LocalDateTime.now().minusMinutes(10))
            assertTrue(dDays().all { it.isNull })
            assertEquals("ONGOING", v2Get(team.master, "/events/${team.eventId}/dashboard").data().at("/displayStatus").asText())

            setSchedule(team.eventId, EventStatus.CLOSED, LocalDateTime.now().minusDays(3))
            assertTrue(dDays().all { it.isNull })
        }
    }

    // ===== 4. O-3 approvalRequired =====

    @Nested
    @DisplayName("O-3 approvalRequired (주문 시점 승인형 여부)")
    inner class ApprovalRequired {

        private fun approvalOf(buyer: User, orderUuid: String): Boolean =
            myOrder(buyer, orderUuid).andExpect { status { isOk() } }.data().at("/approvalRequired").let {
                assertTrue(it.isBoolean, it.toString())
                it.asBoolean()
            }

        @Test
        fun `두둥티켓·무료 승인형은 true, 무료 선착순은 false — 주문 뒤 티켓 설정이 바뀌어도 주문 시점 값`() {
            val shop = Shop()
            val buyer = newBuyer("승인형확인")
            val dudoong = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            val freeApproval = v2OrderOk(buyer, freeBodyOf(shop, freeTicket(shop, approvalRequired = true, name = "무료승인"))).at("/orderUuid").asText()
            val freeTicketId = freeTicket(shop, approvalRequired = false, name = "무료선착순")
            val freeFirstCome = v2OrderOk(buyer, freeBodyOf(shop, freeTicketId)).at("/orderUuid").asText()
            assertTrue(approvalOf(buyer, dudoong))
            assertTrue(approvalOf(buyer, freeApproval))
            assertFalse(approvalOf(buyer, freeFirstCome))

            // 티켓을 승인형으로 바꿔도 이미 확정된 주문은 그대로
            val item = ticketItemRepository.findById(freeTicketId).get()
            ReflectionTestUtils.setField(item, "type", band.gosrock.domain.domains.ticket_item.domain.TicketType.APPROVAL)
            ticketItemRepository.save(item)
            assertFalse(approvalOf(buyer, freeFirstCome))
        }
    }

    // ===== 5. R-6 결제 방식 열 =====

    @Nested
    @DisplayName("R-6 결제 방식 열")
    inner class PaymentChannelColumn {

        @Test
        fun `계좌이체·토스 송금·무료, v1 두둥티켓 주문은 빈 칸 — 입금자명 다음 열`() {
            val shop = Shop()
            fun order(method: String, body: Map<String, Any?>? = null) =
                v2OrderOk(newBuyer("결제$method"), body ?: shopBody(shop, method = method)).at("/orderNo").asText()
            val bank = order("BANK_TRANSFER")
            val toss = order("TOSS_TRANSFER")
            val free = order("FREE", freeBodyOf(shop, freeTicket(shop, approvalRequired = true, name = "무료")))
            val v1 = orderRepository.findByUuidIn(listOf(shop.order(newBuyer("v1주문")))).single().orderNo!!

            val sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }.sheet()
            val headers = sheet.headers()
            assertEquals(headers.indexOf("입금자명") + 1, headers.indexOf("결제 방식"))
            val byNo = sheet.column("주문번호").zip(sheet.column("결제 방식")).toMap()
            assertEquals("계좌이체", byNo[bank])
            assertEquals("토스 송금", byNo[toss])
            assertEquals("무료", byNo[free])
            assertEquals("", byNo[v1])
        }
    }

    // ===== 6. Swagger 문구 =====

    @Nested
    @DisplayName("Swagger 문구")
    inner class SwaggerText {

        private fun docs(): JsonNode =
            objectMapper.readTree(
                mockMvc.get("/v3/api-docs/{group}", "v2-전체") { with(user("1").roles("USER")) }.andExpect { status { isOk() } }
                    .andReturn().response.getContentAsString(Charsets.UTF_8),
            )

        @Test
        fun `P-1·P-2 는 '종료 전(진행 중 포함)' 기준, N-1 은 GIFT 알림·대상 설명 포함`() {
            val docs = docs()
            val ops = docs["paths"].fields().asSequence().flatMap { it.value.fields().asSequence().map { (_, op) -> op } }.toList()
            val p1 = ops.first { it["summary"]?.asText().orEmpty().startsWith("[P-1]") }
            val p2 = ops.first { it["summary"]?.asText().orEmpty().startsWith("[P-2]") }
            assertTrue(p1["summary"].asText().contains("종료 전") && p1["summary"].asText().contains("진행 중"), p1.toString())
            assertFalse(p1["summary"].asText().contains("시작 전 공연 시작"), p1.toString())
            assertTrue(p2["description"].asText().contains("종료 전") && !p2["description"].asText().contains("다가오는 공연(OPEN·시작 전)"), p2.toString())

            val schemas = docs["components"]["schemas"]
            val notificationType = schemas["V2NotificationResponse"]["properties"]["type"]["description"].asText()
            listOf("GIFT_SENT", "GIFT_ACCEPTED", "GIFT_RECEIVED", "GIFT_REJECTED", "GIFT_RETURNED", "GIFT_TICKET_CANCELED").forEach {
                assertTrue(notificationType.contains(it), "$it: $notificationType")
            }
            assertTrue(schemas["V2NotificationTarget"]["properties"]["type"]["description"].asText().contains("GIFT: id = giftId"))
        }
    }

}
