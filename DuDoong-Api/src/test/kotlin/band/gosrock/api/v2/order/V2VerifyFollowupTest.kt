package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.EntityManagerFactory
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/**
 * v2 삼자 검증 후속 (#726): 입금자명 검색·엑셀, 결제 화면 계좌(O-0), 잔여·매진의 승인 대기 차감, 승인형 1인 제한(동시성), 사용자 알림(호스트 취소·환불 완료).
 * [V2UserOrderTestSupport.Shop] = 두둥티켓 6000원(재고 20, 1인 4장, 승인) + 옵션 2개. H2 주의: 승인할 주문은 2장 이하
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 검증 후속 (#726)")
class V2VerifyFollowupTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    private fun uuidsOf(data: JsonNode): List<String> = data.at("/orders/content").map { it.at("/orderUuid").asText() }

    private fun publicTickets(eventId: Long): JsonNode =
        mockMvc.get("/api/v2/events/$eventId/ticket-items").andExpect { status { isOk() } }.data()

    private fun checkout(requester: User?, eventId: Long, ticketItemId: Long) =
        v2Get(requester, "/events/$eventId/ticket-items/$ticketItemId/checkout")

    @Nested
    @DisplayName("R-1·R-6 입금자명 검색·엑셀")
    inner class Depositor {

        @Test
        fun `searchType=DEPOSITOR_NAME 은 주문에 저장된 입금자명 부분일치, 건수도 같은 기준, 이름 검색과 섞이지 않는다`() {
            val shop = Shop()
            val a = v2OrderOk(newBuyer("가나다"), shopBody(shop, depositorName = "128구구")).at("/orderUuid").asText()
            val b = v2OrderOk(newBuyer("라마바"), shopBody(shop, depositorName = "김구구")).at("/orderUuid").asText()
            v2OrderOk(newBuyer("구구"), shopBody(shop, depositorName = "박철수"))
            shop.order(newBuyer("v1주문자")) // v1 주문 (입금자명 null)

            val byDepositor = orders(shop.team.guest, shop.eventId, mapOf("searchType" to "DEPOSITOR_NAME", "keyword" to "구구"))
            assertEquals(setOf(a, b), uuidsOf(byDepositor).toSet())
            assertEquals(2, byDepositor.at("/counts/all").asLong())
            assertEquals(2, byDepositor.at("/counts/pendingApprove").asLong())
            assertEquals(listOf("김구구", "128구구"), byDepositor.at("/orders/content").map { it.at("/depositorName").asText() })
            // 앞뒤 공백은 v1 검색과 같이 무시
            assertEquals(listOf(a), uuidsOf(orders(shop.team.guest, shop.eventId, mapOf("searchType" to "DEPOSITOR_NAME", "keyword" to " 128 "))))
            // 이름 검색은 회원 이름 기준 그대로 (입금자명 '박철수' 주문의 회원 이름이 '구구')
            assertEquals(1, orders(shop.team.guest, shop.eventId, mapOf("keyword" to "구구")).at("/counts/all").asLong())
            assertEquals(0, orders(shop.team.guest, shop.eventId, mapOf("searchType" to "DEPOSITOR_NAME", "keyword" to "v1")).at("/counts/all").asLong())
            v2Get(shop.team.guest, "/events/${shop.eventId}/orders", mapOf("searchType" to "EMAIL", "keyword" to "x")).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `v1 어드민 주문 목록의 검색 종류는 그대로 - DEPOSITOR_NAME 은 받지 않는다`() {
            val shop = Shop()
            shop.order(newBuyer())
            mockMvc.get("/api/v1/events/${shop.eventId}/orders") {
                with(auth(shop.team.master))
                param("orderStage", "APPROVE_WAITING")
                param("searchType", "DEPOSITOR_NAME")
                param("searchString", "구구")
            }.andExpect { status { isBadRequest() } }
            mockMvc.get("/api/v1/events/${shop.eventId}/orders") {
                with(auth(shop.team.master))
                param("orderStage", "APPROVE_WAITING")
                param("searchType", "NAME")
                param("searchString", "구매자")
            }.andExpect { status { isOk() } }
        }

        @Test
        fun `엑셀에 입금자명 열 (연락처 다음), 수식으로 시작하는 입금자명은 작은따옴표, 검색 필터도 같다`() {
            val shop = Shop()
            v2OrderOk(newBuyer("정상"), shopBody(shop, depositorName = "128구구"))
            v2OrderOk(newBuyer("악성"), shopBody(shop, depositorName = "=HYPERLINK(\"x\")"))
            shop.order(newBuyer("v1주문자"))
            val sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }.sheet()
            assertEquals(listOf("주문번호", "주문자", "연락처", "입금자명", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유"), sheet.headers().take(11))
            assertEquals(listOf("", "'=HYPERLINK(\"x\")", "128구구"), sheet.column("입금자명"))
            // 화면(JSON)은 원문
            assertTrue(orders(shop.team.guest, shop.eventId).at("/orders/content").any { it.at("/depositorName").asText() == "=HYPERLINK(\"x\")" })

            val filtered = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export", mapOf("searchType" to "DEPOSITOR_NAME", "keyword" to "구구"))
                .andExpect { status { isOk() } }.sheet()
            assertEquals(listOf("128구구"), filtered.column("입금자명"))
        }
    }

    @Nested
    @DisplayName("O-0 결제 화면 계좌")
    inner class Checkout {

        @Test
        fun `로그인 사용자에게 두둥티켓 계좌·예금주 + P-5 와 같은 티켓, 비로그인 401, 공개 P-5 에는 계좌가 없다`() {
            val shop = Shop()
            val buyer = newBuyer()
            checkout(null, shop.eventId, shop.ticketId).andExpect { status { isUnauthorized() } }

            val data = checkout(buyer, shop.eventId, shop.ticketId).andExpect { status { isOk() } }.data()
            assertEquals("신한은행", data.at("/account/bankName").asText())
            assertEquals("고스락", data.at("/account/accountHolder").asText())
            assertEquals("110-123-456789", data.at("/account/accountNumber").asText())
            val publicItem = publicTickets(shop.eventId).single { it.at("/ticketItemId").asLong() == shop.ticketId }
            assertEquals(publicItem, data.at("/ticket"))

            // 주문 후 O-1 응답 계좌와 같은 형태·값 (토스 송금 링크를 결제 전에 같은 값으로 만든다)
            val order = v2OrderOk(buyer, shopBody(shop, method = "TOSS_TRANSFER"))
            assertEquals(order.at("/payment/account"), data.at("/account"))

            val raw = mockMvc.get("/api/v2/events/${shop.eventId}/ticket-items").andExpect { status { isOk() } }.andReturn().response.getContentAsString(Charsets.UTF_8)
            assertFalse(raw.contains("110-123-456789"), raw)
            assertFalse(raw.contains("account"), raw)
        }

        @Test
        fun `구매할 수 없으면(매진·지난 공연·정산중·종료) 티켓은 보여도 계좌는 null`() {
            val buyer = newBuyer()
            fun account(shop: Shop): JsonNode = checkout(buyer, shop.eventId, shop.ticketId).andExpect { status { isOk() } }.data().let {
                assertFalse(it.at("/ticket/isPurchasable").asBoolean(), it.toString())
                it.at("/account")
            }

            val soldOut = Shop()
            ticketItemRepository.save(ticketItemRepository.findById(soldOut.ticketId).get().also { ReflectionTestUtils.setField(it, "quantity", 1L) })
            assertFalse(checkout(buyer, soldOut.eventId, soldOut.ticketId).andExpect { status { isOk() } }.data().at("/account").isNull)
            v2OrderOk(newBuyer(), shopBody(soldOut)) // 승인 대기 1 = 재고 1 → 매진
            assertTrue(account(soldOut).isNull)

            val started = Shop()
            setEventStart(started.eventId, LocalDateTime.now().minusMinutes(1))
            assertTrue(account(started).isNull)

            for (status in listOf(EventStatus.CALCULATING, EventStatus.CLOSED)) {
                val ended = Shop()
                setEventStatus(ended.eventId, status)
                assertTrue(account(ended).isNull, "$status")
            }
        }

        @Test
        fun `무료 티켓은 계좌 null, 판매 중단·다른 공연 티켓·준비중 공연은 404`() {
            val shop = Shop()
            val buyer = newBuyer()
            val free = freeTicket(shop, approvalRequired = true)
            val freeData = checkout(buyer, shop.eventId, free).andExpect { status { isOk() } }.data()
            assertTrue(freeData.at("/account").isNull)
            assertEquals("FREE", freeData.at("/ticket/payType").asText())

            val other = Shop("남의호스트")
            checkout(buyer, shop.eventId, other.ticketId).andExpect { status { isNotFound() } }
            suspend(shop.team.manager, shop.eventId, shop.ticketId).andExpect { status { isOk() } }
            checkout(buyer, shop.eventId, shop.ticketId).andExpect { status { isNotFound() } }
            setEventStatus(other.eventId, EventStatus.PREPARING)
            checkout(buyer, other.eventId, other.ticketId).andExpect { status { isNotFound() } }
        }
    }

    @Nested
    @DisplayName("P-5 잔여·매진 (승인 대기 차감)")
    inner class Remaining {

        private fun item(eventId: Long, ticketItemId: Long): JsonNode =
            publicTickets(eventId).single { it.at("/ticketItemId").asLong() == ticketItemId }

        @Test
        fun `잔여 = 재고 - 승인 대기, 승인되면 재고가 줄고 대기에서 빠지며, 거절되면 돌아온다`() {
            val shop = Shop()
            val buyerA = newBuyer()
            val a = v2OrderOk(buyerA, shopBody(shop, quantity = 2)).at("/orderUuid").asText()
            val b = v2OrderOk(newBuyer(), shopBody(shop, quantity = 3)).at("/orderUuid").asText()
            assertEquals(15, item(shop.eventId, shop.ticketId).at("/remaining").asLong())
            assertEquals(20, stock(shop.ticketId), "재고는 승인 전까지 그대로 (DEC-020 #1)")

            v1Approve(shop.team.master, shop.eventId, a).andExpect { status { isOk() } }
            assertEquals(15, item(shop.eventId, shop.ticketId).at("/remaining").asLong())
            refuse(shop.team.manager, shop.eventId, b, "SOLD_OUT").andExpect { status { isOk() } }
            assertEquals(18, item(shop.eventId, shop.ticketId).at("/remaining").asLong())
            // 결제 화면(O-0)도 같은 값
            assertEquals(18, checkout(buyerA, shop.eventId, shop.ticketId).andExpect { status { isOk() } }.data().at("/ticket/remaining").asLong())
        }

        @Test
        fun `승인 대기가 재고를 다 채우면 매진·구매 불가이고 주문도 재고 부족 - 다른 티켓 대기는 세지 않는다`() {
            val shop = Shop()
            val item = ticketItemRepository.findById(shop.ticketId).get()
            ReflectionTestUtils.setField(item, "quantity", 3L)
            ticketItemRepository.save(item)
            val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
            v2OrderOk(newBuyer(), orderBody(shop.eventId, other, quantity = 3))

            v2OrderOk(newBuyer(), shopBody(shop, quantity = 2))
            item(shop.eventId, shop.ticketId).let {
                assertEquals(1, it.at("/remaining").asLong())
                assertFalse(it.at("/isSoldOut").asBoolean())
                assertTrue(it.at("/isPurchasable").asBoolean())
            }
            v2OrderOk(newBuyer(), shopBody(shop, quantity = 1))
            item(shop.eventId, shop.ticketId).let {
                assertEquals(0, it.at("/remaining").asLong())
                assertTrue(it.at("/isSoldOut").asBoolean())
                assertFalse(it.at("/isPurchasable").asBoolean())
            }
            // 화면 판정과 주문 재고 검사(#723)가 같은 기준
            assertEquals("Ticket_Item_400_1", v2CreateOrder(newBuyer(), shopBody(shop)).andExpect { status { isBadRequest() } }.code())
            item(shop.eventId, other).let {
                assertEquals(7, it.at("/remaining").asLong())
                assertTrue(it.at("/isPurchasable").asBoolean())
            }
        }

        @Test
        fun `재고 비공개 티켓은 remaining null 이어도 매진은 승인 대기로 판정`() {
            val shop = Shop()
            val hidden = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "비공개", supplyCount = 2, overrides = mapOf("isQuantityPublic" to false)))
            v2OrderOk(newBuyer(), orderBody(shop.eventId, hidden, quantity = 2))
            item(shop.eventId, hidden).let {
                assertTrue(it.at("/remaining").isNull)
                assertTrue(it.at("/isSoldOut").asBoolean())
                assertFalse(it.at("/isPurchasable").asBoolean())
            }
        }
    }

    @Nested
    @DisplayName("호스트 T-1·D-1 승인 대기 수량 (결정 2026-10-05)")
    inner class HostPending {

        private fun manage(shop: Shop): Map<Long, JsonNode> =
            v2Get(shop.team.guest, "/events/${shop.eventId}/ticket-items/manage").andExpect { status { isOk() } }.data()
                .associateBy { it.at("/ticketItemId").asLong() }

        private fun dashboard(shop: Shop): Map<Long, JsonNode> =
            v2Get(shop.team.guest, "/events/${shop.eventId}/dashboard").andExpect { status { isOk() } }.data().at("/tickets/items")
                .associateBy { it.at("/ticketItemId").asLong() }

        @Test
        fun `pendingApproveCount 는 티켓별 승인 대기 수량, 기존 remaining·soldCount·hasPendingOrders 의미는 그대로`() {
            val shop = Shop()
            val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
            v2OrderOk(newBuyer(), shopBody(shop, quantity = 2))
            v2OrderOk(newBuyer(), shopBody(shop, quantity = 3))
            shop.approved(newBuyer(), quantity = 1)

            manage(shop).let {
                val t = it.getValue(shop.ticketId)
                assertEquals(5, t.at("/pendingApproveCount").asLong())
                assertEquals(19, t.at("/remaining").asLong(), "remaining 은 재고 그대로 (승인 1장만 감소)")
                assertEquals(1, t.at("/soldCount").asLong())
                assertTrue(t.at("/hasPendingOrders").asBoolean())
                val o = it.getValue(other)
                assertEquals(0, o.at("/pendingApproveCount").asLong())
                assertFalse(o.at("/hasPendingOrders").asBoolean())
            }
            dashboard(shop).let {
                assertEquals(5, it.getValue(shop.ticketId).at("/pendingApproveCount").asLong())
                assertEquals(1, it.getValue(shop.ticketId).at("/soldCount").asLong())
                assertEquals(0, it.getValue(other).at("/pendingApproveCount").asLong())
            }
            // 사용자 P-5 잔여 = 호스트 remaining - pendingApproveCount
            assertEquals(14, publicTickets(shop.eventId).single { it.at("/ticketItemId").asLong() == shop.ticketId }.at("/remaining").asLong())
            // 단건 응답(변경 API)도 같은 값
            val patched = v2Post(shop.team.manager, "/events/${shop.eventId}/ticket-items/$other/suspend").andExpect { status { isOk() } }.data()
            assertEquals(0, patched.at("/pendingApproveCount").asLong())
        }

        /**
         * 승인 대기 수량은 티켓 수와 관계없이 그룹 쿼리 1개. JPQL/QueryDSL 쿼리 실행 수(queryExecutionCount)로 본다 —
         * T-1 은 티켓마다 옵션 컬렉션 지연 로딩(기존 동작, #726 범위 밖)이 있어 SQL 문 수(prepareStatementCount)는 티켓 수에 따라 는다
         */
        @Test
        fun `N+1 없음 - 티켓 수와 관계없이 T-1·D-1 조회 쿼리 수가 같다 (승인 대기는 그룹 쿼리 1개)`() {
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            // 통계는 컨텍스트 전체 공유라 다른 테스트의 비동기 알림 저장이 섞일 수 있다 → 3번 재고 최솟값 (잡음은 더하기만 한다)
            fun countQueries(block: () -> Unit): Long = (1..3).minOf {
                statistics.isStatisticsEnabled = true
                try {
                    statistics.clear()
                    block()
                    statistics.queryExecutionCount
                } finally {
                    statistics.isStatisticsEnabled = false
                }
            }
            val shop = Shop()
            v2OrderOk(newBuyer(), shopBody(shop))
            val manageOne = countQueries { manage(shop) }
            val dashboardOne = countQueries { dashboard(shop) }
            repeat(3) {
                val t = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "추가$it", supplyCount = 10))
                v2OrderOk(newBuyer(), orderBody(shop.eventId, t))
            }
            assertEquals(4, manage(shop).size)
            assertEquals(manageOne, countQueries { manage(shop) }, "T-1 티켓 1개: $manageOne")
            assertEquals(dashboardOne, countQueries { dashboard(shop) }, "D-1 티켓 1개: $dashboardOne")
        }
    }

    @Nested
    @DisplayName("승인형 1인 제한 (승인 대기 합산)")
    inner class PurchaseLimit {

        private fun concurrently(n: Int, block: (Int) -> String): List<String> {
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(n)
            val results = Collections.synchronizedList(mutableListOf<String>())
            repeat(n) { i ->
                pool.submit {
                    start.await()
                    results += runCatching { block(i) }.getOrElse { "ERR:$it" }
                }
            }
            start.countDown()
            pool.shutdown()
            assertTrue(pool.awaitTermination(60, TimeUnit.SECONDS))
            return results
        }

        private fun respond(buyer: User, body: Map<String, Any?>): String =
            v2CreateOrder(buyer, body).andReturn().response.let {
                if (it.status == 200) "200" else objectMapper.readTree(it.getContentAsString(Charsets.UTF_8)).at("/code").asText()
            }

        @Test
        fun `같은 사용자의 승인 대기 2건이 동시에 와서 합이 제한(4)을 넘으면 1건만 접수 (같은 티켓 락)`() {
            repeat(3) {
                val shop = Shop()
                val buyer = newBuyer()
                // 2장 + 3장 = 5 > 4. 수량이 달라 중복 요청으로 묶이지 않는다
                val results = concurrently(2) { i -> respond(buyer, shopBody(shop, quantity = if (i == 0) 2 else 3)) }
                assertEquals(1, results.count { it == "200" }, "$results")
                assertEquals(listOf("Order_400_15"), results.filter { it != "200" }, "$results")
                val pending = myOrders(buyer).at("/content").sumOf { it.at("/quantity").asLong() }
                assertTrue(pending in 2..3, "pending=$pending $results")
            }
        }

        @Test
        fun `제한 안의 동시 주문은 모두 접수, 다른 티켓의 대기는 이 티켓 제한에 세지 않는다`() {
            val shop = Shop()
            val buyer = newBuyer()
            val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓", supplyCount = 10))
            v2OrderOk(buyer, orderBody(shop.eventId, other, quantity = 4))
            val results = concurrently(2) { i -> respond(buyer, shopBody(shop, quantity = 2, yes = i == 0)) }
            assertEquals(listOf("200", "200"), results)
            assertEquals("Order_400_15", respond(buyer, shopBody(shop)))
        }

        @Test
        fun `v1 주문 경로도 같은 승인 대기 합산 검사 (v1 동작 고정)`() {
            val shop = Shop()
            val buyer = newBuyer()
            shop.order(buyer, quantity = 2)
            shop.order(buyer, quantity = 2)
            val answers = v1Answers(buyer, shop.eventId, shop.ticketId)
            val cartId = v1Cart(buyer, shop.ticketId, 1, answers).andExpect { status { isOk() } }.data().at("/cartId").asLong()
            assertEquals("Order_400_15", v1CreateOrder(buyer, cartId).andExpect { status { isBadRequest() } }.code())
            // v1 승인 대기 4장이 있으면 v2 주문도 막힌다 (같은 합산 검사)
            assertEquals("Order_400_15", respond(buyer, shopBody(shop)))
        }
    }

    @Nested
    @DisplayName("사용자 알림 - 호스트 취소·환불 완료")
    inner class Notifications {

        private fun of(user: User, type: NotificationType, target: String? = null) =
            notificationRepository.findAllByUserId(user.id!!).filter { it.type == type && (target == null || it.targetId == target) }

        private fun count(user: User, type: NotificationType, target: String? = null) = of(user, type, target).size

        /** 처음 보일 때까지 기다린다 (최대 10초) */
        private fun arrived(user: User, type: NotificationType, target: String? = null): Boolean {
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline) {
                if (count(user, type, target) > 0) return true
                Thread.sleep(50)
            }
            return false
        }

        /** 처음 보일 때까지 기다린 뒤 [STABLE_MS] 더 기다려 최종 개수를 센다 (뒤늦은 중복 저장도 잡는다) */
        private fun await(user: User, type: NotificationType, target: String? = null): Int {
            arrived(user, type, target)
            Thread.sleep(STABLE_MS)
            return count(user, type, target)
        }

        /**
         * '알림 없음' 검증의 기준점: 같은 사용자에게 **나중에** 일어난 알림(새 주문 승인 → 호스트 취소)이 도착할 때까지 기다린다.
         * 알림 전용 풀은 큐 순서대로 꺼내므로, 기준 알림이 보이면 앞서 일어난 상태 변경의 알림 처리는 시작됐다 (고정 대기 대신)
         */
        private fun laterReference(shop: Shop, user: User) {
            val reference = shop.approved(user)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$reference/cancel").andExpect { status { isOk() } }
            assertTrue(arrived(user, NotificationType.ORDER_CANCELED_BY_HOST, reference), "기준 알림이 도착하지 않음")
        }

        /** 운영 어드민(/internal-api)은 SecurityConfig 에서 ADMIN 이상 역할을 본다 (DB 역할은 UseCase 의 AdminAuthValidator) */
        private fun adminAuth(u: User) = SecurityMockMvcRequestPostProcessors.user(u.id.toString()).roles("SUPER_ADMIN")

        private fun only(user: User, type: NotificationType, target: String? = null) = of(user, type, target).single()

        private fun complete(requester: User, eventId: Long, orderUuid: String) =
            v2Post(requester, "/events/$eventId/refunds/$orderUuid/complete").andExpect { status { isOk() } }

        private fun v1Complete(shop: Shop, orderUuid: String) =
            mockMvc.patch("/api/v1/events/${shop.eventId}/refunds/$orderUuid/complete") { with(auth(shop.team.master)) }.andExpect { status { isOk() } }

        private fun v1Cancel(shop: Shop, orderUuid: String, reason: String) =
            mockMvc.post("/api/v1/events/${shop.eventId}/orders/$orderUuid/cancel") {
                with(auth(shop.team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("reason" to reason))
            }.andExpect { status { isOk() } }

        @Test
        fun `v2 승인 후 호스트 취소 → 주문자 ORDER_CANCELED_BY_HOST (사유 포함), 거절 알림은 없다`() {
            val shop = Shop()
            val buyer = newBuyer()
            val order = shop.approved(buyer)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$order/cancel", mapOf("reason" to "공연 취소")).andExpect { status { isOk() } }
            assertEquals(1, await(buyer, NotificationType.ORDER_CANCELED_BY_HOST))
            val n = only(buyer, NotificationType.ORDER_CANCELED_BY_HOST)
            assertEquals(order, n.targetId)
            // v1 앱 주문(결제 채널 없음)이라 환불 계좌 입력 안내는 붙지 않는다 (#728 v2 주문만)
            assertTrue(n.body.contains("호스트에 의해 취소") && n.body.endsWith("사유: 공연 취소"), n.body)
            laterReference(shop, buyer)
            assertEquals(0, count(buyer, NotificationType.ORDER_REFUSED))
            // 호스트 쪽(마스터·매니저)에는 보내지 않는다
            assertEquals(0, count(shop.team.manager, NotificationType.ORDER_CANCELED_BY_HOST))
        }

        @Test
        fun `v1 호스트 취소 경로도 같은 알림, 승인 대기 거절은 거절 알림만`() {
            val shop = Shop()
            val buyer = newBuyer()
            val order = shop.approved(buyer)
            v1Cancel(shop, order, "v1 취소")
            assertEquals(1, await(buyer, NotificationType.ORDER_CANCELED_BY_HOST))

            val refusedBuyer = newBuyer()
            val refused = shop.order(refusedBuyer)
            refuse(shop.team.manager, shop.eventId, refused, "SOLD_OUT").andExpect { status { isOk() } }
            assertTrue(arrived(refusedBuyer, NotificationType.ORDER_REFUSED, refused))
            laterReference(shop, refusedBuyer)
            assertEquals(0, count(refusedBuyer, NotificationType.ORDER_CANCELED_BY_HOST, refused))
        }

        @Test
        fun `무료 선착순(결제형) 주문도 호스트가 취소하면 ORDER_CANCELED_BY_HOST - v2·v1 경로, 사용자 본인 취소는 대상 아님 (결정 2026-10-05)`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false)
            val v2Buyer = newBuyer()
            val v2Order = v2OrderOk(v2Buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$v2Order/cancel", mapOf("reason" to "무료 취소")).andExpect { status { isOk() } }
            assertEquals(1, await(v2Buyer, NotificationType.ORDER_CANCELED_BY_HOST, v2Order))

            val v1Buyer = newBuyer()
            val v1Order = v2OrderOk(v1Buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            v1Cancel(shop, v1Order, "v1 무료 취소")
            assertEquals(1, await(v1Buyer, NotificationType.ORDER_CANCELED_BY_HOST, v1Order))
            assertTrue(only(v1Buyer, NotificationType.ORDER_CANCELED_BY_HOST, v1Order).body.endsWith("사유: v1 무료 취소"))

            val self = newBuyer()
            val selfOrder = v2OrderOk(self, freeBodyOf(shop, free)).at("/orderUuid").asText()
            cancelMy(self, selfOrder).andExpect { status { isOk() } }
            assertTrue(arrived(shop.team.manager, NotificationType.ORDER_CANCELED_BY_USER, selfOrder))
            laterReference(shop, self)
            assertEquals(0, count(self, NotificationType.ORDER_CANCELED_BY_HOST, selfOrder))
        }

        @Test
        fun `v2 환불 완료 → 주문자 ORDER_REFUND_COMPLETED 1건 (다시 눌러도 멱등), 사용자 환불 요청 주문도 같다`() {
            val shop = Shop()
            val buyer = newBuyer()
            val refused = shop.order(buyer)
            refuse(shop.team.manager, shop.eventId, refused, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } }
            complete(shop.team.manager, shop.eventId, refused)
            assertEquals(1, await(buyer, NotificationType.ORDER_REFUND_COMPLETED))
            complete(shop.team.manager, shop.eventId, refused)
            laterReference(shop, buyer)
            assertEquals(1, count(buyer, NotificationType.ORDER_REFUND_COMPLETED))
            val n = only(buyer, NotificationType.ORDER_REFUND_COMPLETED)
            assertEquals(refused, n.targetId)
            assertEquals(shop.eventId, n.eventId)

            val requester = newBuyer()
            val paid = v2OrderOk(requester, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(requester, paid, refundAccount).andExpect { status { isOk() } }
            complete(shop.team.manager, shop.eventId, paid)
            assertEquals(1, await(requester, NotificationType.ORDER_REFUND_COMPLETED))
        }

        @Test
        fun `v1 환불 완료를 두 번 호출해도 알림 1건 (v1 은 상태 검사 없이 매번 이벤트 발행 → uk 로 중복 제거)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val order = shop.approved(buyer)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$order/cancel", mapOf("reason" to "x")).andExpect { status { isOk() } }
            v1Complete(shop, order)
            assertTrue(arrived(buyer, NotificationType.ORDER_REFUND_COMPLETED, order))
            v1Complete(shop, order)
            laterReference(shop, buyer)
            assertEquals(1, count(buyer, NotificationType.ORDER_REFUND_COMPLETED, order))
        }

        @Test
        fun `승인 완료(APPROVED) 주문을 v1·운영에서 잘못 환불 완료 처리하면 알림 없음, 돌려줄 돈 없는 무료 주문도 없음`() {
            val shop = Shop()
            val buyer = newBuyer()
            val approved = shop.approved(buyer)
            v1Complete(shop, approved) // v1 은 상태 검사 없이 환불 완료로 바꾼다
            mockMvc.patch("/internal-api/v1/refunds/$approved/complete") { with(adminAuth(superAdmin())) }.andExpect { status { isOk() } }
            laterReference(shop, buyer)
            assertEquals(0, count(buyer, NotificationType.ORDER_REFUND_COMPLETED, approved))

            val freeBuyer = newBuyer()
            val free = freeTicket(shop, approvalRequired = true)
            val freeOrder = v2OrderOk(freeBuyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, freeOrder, "SOLD_OUT").andExpect { status { isOk() } }
            complete(shop.team.manager, shop.eventId, freeOrder)
            laterReference(shop, freeBuyer)
            assertEquals(0, count(freeBuyer, NotificationType.ORDER_REFUND_COMPLETED))
        }

        @Test
        fun `운영 어드민 경로(취소, 환불 확인, 환불 상태 변경)도 같은 알림 - Admin 모듈이 같은 도메인 이벤트를 발행`() {
            val shop = Shop()
            val admin = superAdmin()
            val buyer = newBuyer()
            val order = shop.approved(buyer)
            mockMvc.post("/internal-api/v1/orders/$order/cancel") {
                with(adminAuth(admin))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("reason" to "운영 취소"))
            }.andExpect { status { isOk() } }
            assertEquals(1, await(buyer, NotificationType.ORDER_CANCELED_BY_HOST))
            assertTrue(only(buyer, NotificationType.ORDER_CANCELED_BY_HOST).body.endsWith("사유: 운영 취소"))
            mockMvc.patch("/internal-api/v1/refunds/$order/complete") { with(adminAuth(admin)) }.andExpect { status { isOk() } }
            assertEquals(1, await(buyer, NotificationType.ORDER_REFUND_COMPLETED))

            val other = newBuyer()
            val refused = shop.order(other)
            refuse(shop.team.manager, shop.eventId, refused, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } }
            mockMvc.patch("/internal-api/v1/orders/$refused/refund-status") {
                with(adminAuth(admin))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("refundStatus" to "REFUND_COMPLETED"))
            }.andExpect { status { isOk() } }
            assertEquals(1, await(other, NotificationType.ORDER_REFUND_COMPLETED))
            assertEquals(0, count(other, NotificationType.ORDER_CANCELED_BY_HOST, refused), "거절 주문은 호스트 취소 알림 대상 아님")
        }

        @Test
        fun `v1 환불 완료 경로도 같은 알림`() {
            val shop = Shop()
            val buyer = newBuyer()
            val order = shop.approved(buyer)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$order/cancel", mapOf("reason" to "x")).andExpect { status { isOk() } }
            v1Complete(shop, order)
            assertEquals(1, await(buyer, NotificationType.ORDER_REFUND_COMPLETED))
        }
    }

    companion object {
        private const val STABLE_MS = 500L
    }
}
