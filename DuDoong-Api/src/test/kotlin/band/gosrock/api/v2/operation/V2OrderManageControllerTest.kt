package band.gosrock.api.v2.operation

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.PaymentInfo
import band.gosrock.domain.domains.order.service.v2.V2OrderDomainService
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

/** v2 공연 운영 - 대시보드·주문·환불 통합 테스트 (#712): D-1, R-1 ~ R-6, F-1, F-2 + 권한 경계 + v1 호환 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 공연 운영 - 주문")
class V2OrderManageControllerTest : V2OperationTestSupport() {

    @Autowired private lateinit var v2OrderDomainService: V2OrderDomainService

    /** 승인 대기 1, 승인 1, v2 거절 1, v1 거절 1, 승인 후 v2 취소 1 */
    private inner class Mixed {
        val shop = Shop()
        val pendingBuyer = newBuyer("대기자", "010-1111-2222")
        val approvedBuyer = newBuyer("승인자", "010-3333-4444")
        val refusedBuyer = newBuyer("거절자")
        val v1RefusedBuyer = newBuyer("옛거절자")
        val canceledBuyer = newBuyer("취소자")
        val pending = shop.order(pendingBuyer)
        val approved = shop.approved(approvedBuyer, quantity = 2)
        val refused = shop.order(refusedBuyer).also {
            refuse(shop.team.manager, shop.eventId, it, "SOLD_OUT").andExpect { status { isOk() } }
        }
        val v1Refused = shop.order(v1RefusedBuyer).also {
            mockMvc.post("/api/v1/events/${shop.eventId}/orders/$it/refuse") {
                with(auth(shop.team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("reason" to "v1 사유"))
            }.andExpect { status { isOk() } }
        }
        val canceled = shop.approved(canceledBuyer).also {
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$it/cancel", mapOf("reason" to " 일정 변경 ")).andExpect { status { isOk() } }
        }
    }

    @Nested
    @DisplayName("R-1 목록 / 상태 분류 / 건수")
    inner class ListOrders {

        @Test
        fun `상태 분류 - v2 거절·v1 거절은 REFUSED, 승인 후 취소는 CANCELED, 건수 포함`() {
            val m = Mixed()
            val data = orders(m.shop.team.guest, m.shop.eventId)
            val counts = data.at("/counts")
            assertEquals(5, counts.at("/all").asLong())
            assertEquals(1, counts.at("/pendingApprove").asLong())
            assertEquals(1, counts.at("/approved").asLong())
            assertEquals(2, counts.at("/refused").asLong())
            assertEquals(1, counts.at("/canceled").asLong())
            assertEquals(0, counts.at("/failed").asLong())
            val byUuid = data.at("/orders/content").associateBy { it.at("/orderUuid").asText() }
            assertEquals(5, byUuid.size)
            assertEquals(5, data.at("/orders/totalElements").asLong())

            // 최신 순
            assertEquals(m.canceled, data.at("/orders/content/0/orderUuid").asText())
            byUuid.getValue(m.approved).let {
                assertEquals("APPROVED", it.at("/status").asText())
                assertEquals("승인자", it.at("/buyerName").asText())
                assertEquals("010-3333-4444", it.at("/buyerPhone").asText())
                assertEquals(2, it.at("/totalQuantity").asLong())
                // (6000 + 옵션 '예' 1000) x 2
                assertEquals(14000, it.at("/totalPaymentAmount").asLong())
                assertEquals("NONE", it.at("/refundStatus").asText())
                assertTrue(it.at("/orderNo").asText().startsWith("R"))
            }
            byUuid.getValue(m.refused).let {
                assertEquals("REFUSED", it.at("/status").asText())
                assertEquals("SOLD_OUT", it.at("/refuseReasonType").asText())
                assertEquals("티켓 매진", it.at("/refuseReason").asText())
                assertEquals("REQUESTED", it.at("/refundStatus").asText())
                assertTrue(it.at("/cancelReason").isNull)
            }
            byUuid.getValue(m.v1Refused).let {
                assertEquals("REFUSED", it.at("/status").asText())
                assertTrue(it.at("/refuseReasonType").isNull)
                assertEquals("v1 사유", it.at("/refuseReason").asText())
            }
            byUuid.getValue(m.canceled).let {
                assertEquals("CANCELED", it.at("/status").asText())
                assertEquals("일정 변경", it.at("/cancelReason").asText())
                assertTrue(it.at("/refuseReason").isNull)
            }
            assertEquals("PENDING_APPROVE", byUuid.getValue(m.pending).at("/status").asText())
        }

        @Test
        fun `상태 필터·검색(이름, 연락처)·페이징 - 건수는 검색어 반영, 상태 필터 무시`() {
            val m = Mixed()
            val refused = orders(m.shop.team.guest, m.shop.eventId, mapOf("status" to "REFUSED"))
            assertEquals(setOf(m.refused, m.v1Refused), refused.at("/orders/content").map { it.at("/orderUuid").asText() }.toSet())
            assertEquals(5, refused.at("/counts/all").asLong())
            assertEquals(listOf(m.canceled), orders(m.shop.team.guest, m.shop.eventId, mapOf("status" to "CANCELED")).at("/orders/content").map { it.at("/orderUuid").asText() })

            val byName = orders(m.shop.team.guest, m.shop.eventId, mapOf("keyword" to "승인"))
            assertEquals(listOf(m.approved), byName.at("/orders/content").map { it.at("/orderUuid").asText() })
            assertEquals(1, byName.at("/counts/all").asLong())
            assertEquals(1, byName.at("/counts/approved").asLong())
            assertEquals(0, byName.at("/counts/refused").asLong())
            val byPhone = orders(m.shop.team.guest, m.shop.eventId, mapOf("searchType" to "PHONE", "keyword" to "1111-2222"))
            assertEquals(listOf(m.pending), byPhone.at("/orders/content").map { it.at("/orderUuid").asText() })

            val page = orders(m.shop.team.guest, m.shop.eventId, mapOf("page" to "1", "size" to "2"))
            assertEquals(2, page.at("/orders/content").size())
            assertEquals(5, page.at("/orders/totalElements").asLong())
            assertEquals(3, page.at("/orders/totalPages").asInt())
            assertTrue(page.at("/orders/hasNext").asBoolean())

            v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/orders", mapOf("status" to "NOPE")).andExpect { status { isBadRequest() } }
            v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/orders", mapOf("size" to "101")).andExpect { status { isBadRequest() } }
        }

        @Test
        fun `결제 진행 중(PENDING_PAYMENT) 주문은 목록·건수에서 제외, FAILED 는 주문 실패`() {
            val shop = Shop()
            val free = createTicket(shop.team.manager, shop.eventId, freeBody(name = "무료", supplyCount = 10))
            val buyer = newBuyer("무료구매")
            v1Order(buyer, shop.eventId, free) // PENDING_PAYMENT (무료 확정 전)
            val failed = shop.order(newBuyer("실패"))
            orderRepository.findByOrderUuid(failed).get().also { it.fail("테스트") }.let { orderRepository.save(it) }
            val data = orders(shop.team.guest, shop.eventId)
            assertEquals(1, data.at("/counts/all").asLong())
            assertEquals(1, data.at("/counts/failed").asLong())
            assertEquals("FAILED", data.at("/orders/content/0/status").asText())
        }
    }

    @Nested
    @DisplayName("R-2 상세")
    inner class Detail {

        @Test
        fun `주문 라인·발급 티켓별 옵션 응답, 연락처·이메일`() {
            val shop = Shop()
            val buyer = newBuyer("상세", "010-5555-6666")
            val pending = shop.order(buyer, quantity = 2)
            v2Get(shop.team.guest, "/events/${shop.eventId}/orders/$pending").andExpect {
                status { isOk() }
                jsonPath("$.data.order.status") { value("PENDING_APPROVE") }
                jsonPath("$.data.order.buyerPhone") { value("010-5555-6666") }
                jsonPath("$.data.buyerEmail") { value(buyer.profile!!.email!!) }
                jsonPath("$.data.lines.length()") { value(1) }
                jsonPath("$.data.lines[0].quantity") { value(2) }
                jsonPath("$.data.lines[0].unitPrice") { value(6000) }
                jsonPath("$.data.lines[0].linePrice") { value(14000) }
                jsonPath("$.data.lines[0].optionAnswers.length()") { value(2) }
                jsonPath("$.data.issuedTickets.length()") { value(0) }
            }
            val detail = v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$pending/approve").andExpect { status { isOk() } }.data()
            assertEquals("APPROVED", detail.at("/order/status").asText())
            val tickets = detail.at("/issuedTickets")
            assertEquals(2, tickets.size())
            tickets.forEach { t ->
                assertEquals("BEFORE", t.at("/entrance").asText())
                assertTrue(t.at("/issuedTicketNo").asText().startsWith("T"))
                val answers = t.at("/optionAnswers").associate { it.at("/optionName").asText() to it.at("/answer").asText() }
                assertEquals(mapOf("뒷풀이" to "예", "입금자명" to "홍길동"), answers)
            }
            assertEquals(1000, tickets[0].at("/optionAnswers").first { it.at("/optionName").asText() == "뒷풀이" }.at("/additionalPrice").asLong())
        }
    }

    @Nested
    @DisplayName("R-3 승인 / R-4 거절 / R-5 취소")
    inner class Change {

        @Test
        fun `거절 사유 검증 - 종류 필수, 기타는 1~20자`() {
            val shop = Shop()
            val order = shop.order(newBuyer())
            refuse(shop.team.manager, shop.eventId, order, null).andExpect { status { isBadRequest() } }
            refuse(shop.team.manager, shop.eventId, order, "WRONG").andExpect { status { isBadRequest() } }
            assertEquals("Order_400_18", refuse(shop.team.manager, shop.eventId, order, "ETC").andExpect { status { isBadRequest() } }.code())
            assertEquals("Order_400_18", refuse(shop.team.manager, shop.eventId, order, "ETC", "   ").andExpect { status { isBadRequest() } }.code())
            assertEquals("Order_400_18", refuse(shop.team.manager, shop.eventId, order, "ETC", "가".repeat(21)).andExpect { status { isBadRequest() } }.code())
            // 실패한 요청은 상태를 바꾸지 않는다
            assertEquals(OrderStatus.PENDING_APPROVE, orderRepository.findByOrderUuid(order).get().orderStatus)

            refuse(shop.team.manager, shop.eventId, order, "ETC", " ${"가".repeat(20)} ").andExpect {
                status { isOk() }
                jsonPath("$.data.order.status") { value("REFUSED") }
                jsonPath("$.data.order.refuseReasonType") { value("ETC") }
                jsonPath("$.data.order.refuseReason") { value("가".repeat(20)) }
            }
            val saved = orderRepository.findByOrderUuid(order).get()
            assertEquals(OrderStatus.CANCELED, saved.orderStatus)
            assertEquals(OrderRefuseReasonType.ETC, saved.refuseReasonType)
            assertEquals("가".repeat(20), saved.cancelReason)
            assertEquals(RefundStatus.REFUND_REQUESTED, saved.refundStatus)
        }

        @Test
        fun `이미 처리된 주문 - 승인·거절 400, 승인 대기 취소 400`() {
            val shop = Shop()
            val approved = shop.approved(newBuyer())
            assertEquals("Order_400_3", v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$approved/approve").andExpect { status { isBadRequest() } }.code())
            assertEquals("Order_400_5", refuse(shop.team.manager, shop.eventId, approved, "SOLD_OUT").andExpect { status { isBadRequest() } }.code())
            val refused = shop.order(newBuyer()).also { refuse(shop.team.manager, shop.eventId, it, "AMOUNT_MISMATCH").andExpect { status { isOk() } } }
            assertEquals("Order_400_3", v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$refused/approve").andExpect { status { isBadRequest() } }.code())
            val pending = shop.order(newBuyer())
            assertEquals("Order_400_5", v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$pending/cancel").andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `승인과 거절을 동시에 요청하면 하나만 성공 (같은 주문 락)`() {
            val shop = Shop()
            repeat(3) { round ->
                val order = shop.order(newBuyer("동시$round"))
                val start = CountDownLatch(1)
                val pool = Executors.newFixedThreadPool(2)
                val outcomes = Collections.synchronizedMap(mutableMapOf<String, String>())
                pool.submit {
                    start.await()
                    outcomes["approve"] = runCatching { v2OrderDomainService.approve(shop.eventId, order) }.fold({ "OK" }, { codeOf(it) })
                }
                pool.submit {
                    start.await()
                    outcomes["refuse"] = runCatching { v2OrderDomainService.refuse(shop.eventId, order, OrderRefuseReasonType.SOLD_OUT, null) }.fold({ "OK" }, { codeOf(it) })
                }
                start.countDown()
                pool.shutdown()
                assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
                assertEquals(1, outcomes.values.count { it == "OK" }, "round=$round $outcomes")
                val saved = orderRepository.findByOrderUuid(order).get()
                val tickets = issuedTicketRepository.findAllByOrderUuid(order).filter { it.issuedTicketStatus != IssuedTicketStatus.CANCELED }
                if (outcomes["approve"] == "OK") {
                    assertEquals(OrderStatus.APPROVED, saved.orderStatus)
                    assertEquals("Order_400_5", outcomes["refuse"])
                    assertEquals(1, tickets.size)
                } else {
                    assertEquals(OrderStatus.CANCELED, saved.orderStatus)
                    assertEquals(OrderRefuseReasonType.SOLD_OUT, saved.refuseReasonType)
                    assertEquals("Order_400_3", outcomes["approve"])
                    assertEquals(0, tickets.size)
                }
            }
        }

        private fun codeOf(e: Throwable): String = (e as? DuDoongCodeException)?.errorCode?.getErrorReason()?.code ?: e.toString()

        @Test
        fun `취소 - 발급 티켓 취소, 재고 복구, 환불 요청 (사유 없이도 가능)`() {
            val shop = Shop()
            val order = shop.approved(newBuyer(), quantity = 2)
            assertEquals(18, ticketItemRepository.findById(shop.ticketId).get().quantity)
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$order/cancel").andExpect {
                status { isOk() }
                jsonPath("$.data.order.status") { value("CANCELED") }
                jsonPath("$.data.order.refundStatus") { value("REQUESTED") }
                jsonPath("$.data.issuedTickets[0].entrance") { value("CANCELED") }
                jsonPath("$.data.issuedTickets[1].entrance") { value("CANCELED") }
            }
            assertEquals(20, ticketItemRepository.findById(shop.ticketId).get().quantity)
        }

        @Test
        fun `권한 - 승인·거절·취소는 매니저 이상, 일반 멤버·외부인 403, 비로그인 401, SUPER_ADMIN 가능`() {
            val shop = Shop()
            val o1 = shop.order(newBuyer())
            for (who in listOf(shop.team.guest, shop.team.outsider)) {
                v2Post(who, "/events/${shop.eventId}/orders/$o1/approve").andExpect { status { isForbidden() } }
                refuse(who, shop.eventId, o1, "SOLD_OUT").andExpect { status { isForbidden() } }
                v2Post(who, "/events/${shop.eventId}/orders/$o1/cancel").andExpect { status { isForbidden() } }
            }
            v2Get(shop.team.outsider, "/events/${shop.eventId}/orders").andExpect { status { isForbidden() } }
            v2Get(shop.team.outsider, "/events/${shop.eventId}/orders/$o1").andExpect { status { isForbidden() } }
            v2Get(null, "/events/${shop.eventId}/orders").andExpect { status { isUnauthorized() } }
            v2Post(null, "/events/${shop.eventId}/orders/$o1/approve").andExpect { status { isUnauthorized() } }
            assertEquals(OrderStatus.PENDING_APPROVE, orderRepository.findByOrderUuid(o1).get().orderStatus)

            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$o1/approve").andExpect { status { isOk() } }
            val o2 = shop.order(newBuyer())
            val admin = superAdmin()
            refuse(admin, shop.eventId, o2, "DEPOSIT_UNCONFIRMED").andExpect {
                status { isOk() }
                jsonPath("$.data.order.refuseReason") { value("입금 미확인") }
            }
            v2Get(admin, "/events/${shop.eventId}/orders").andExpect { status { isOk() } }
            v2Get(admin, "/events/999999999/orders").andExpect { status { isNotFound() } }
        }

        @Test
        fun `IDOR - 다른 호스트 eventId 는 403, 다른 공연 orderUuid 를 내 eventId 로 보내면 404`() {
            val mine = Shop()
            val other = Shop("남의호스트")
            val otherOrder = other.order(newBuyer())
            val myOrder = mine.order(newBuyer())
            // 다른 호스트 공연 경로 → 권한 없음
            v2Post(mine.team.manager, "/events/${other.eventId}/orders/$otherOrder/approve").andExpect { status { isForbidden() } }
            v2Get(mine.team.manager, "/events/${other.eventId}/orders").andExpect { status { isForbidden() } }
            // 내 공연 경로 + 남의 주문 → 404 (존재를 드러내지 않음), 상태 변화 없음
            val base = "/events/${mine.eventId}/orders/$otherOrder"
            assertEquals("Order_404_1", v2Get(mine.team.manager, base).andExpect { status { isNotFound() } }.code())
            v2Post(mine.team.manager, "$base/approve").andExpect { status { isNotFound() } }
            refuse(mine.team.manager, mine.eventId, otherOrder, "SOLD_OUT").andExpect { status { isNotFound() } }
            v2Post(mine.team.manager, "$base/cancel").andExpect { status { isNotFound() } }
            v2Post(mine.team.manager, "/events/${mine.eventId}/refunds/$otherOrder/complete").andExpect { status { isNotFound() } }
            assertEquals(OrderStatus.PENDING_APPROVE, orderRepository.findByOrderUuid(otherOrder).get().orderStatus)
            // SUPER_ADMIN 도 경로-주문 불일치는 404
            v2Post(superAdmin(), "/events/${mine.eventId}/orders/$otherOrder/approve").andExpect { status { isNotFound() } }
            assertEquals(1, orders(mine.team.guest, mine.eventId).at("/counts/all").asLong())
            assertEquals(myOrder, orders(mine.team.guest, mine.eventId).at("/orders/content/0/orderUuid").asText())
        }
    }

    @Nested
    @DisplayName("F-1 환불 목록 / F-2 환불 완료")
    inner class Refund {

        @Test
        fun `거절·취소 주문은 환불 요청, 매니저가 완료 (멱등), 요청 없는 주문 400`() {
            val m = Mixed()
            val requested = v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/refunds", mapOf("status" to "REQUESTED")).andExpect { status { isOk() } }.data()
            assertEquals(setOf(m.refused, m.v1Refused, m.canceled), requested.at("/content").map { it.at("/orderUuid").asText() }.toSet())
            assertEquals("티켓 매진", requested.at("/content").first { it.at("/orderUuid").asText() == m.refused }.at("/reason").asText())

            v2Post(m.shop.team.guest, "/events/${m.shop.eventId}/refunds/${m.refused}/complete").andExpect { status { isForbidden() } }
            v2Post(m.shop.team.manager, "/events/${m.shop.eventId}/refunds/${m.refused}/complete").andExpect {
                status { isOk() }
                jsonPath("$.data.order.refundStatus") { value("COMPLETED") }
                jsonPath("$.data.order.status") { value("REFUSED") }
            }
            v2Post(m.shop.team.manager, "/events/${m.shop.eventId}/refunds/${m.refused}/complete").andExpect { status { isOk() } }
            assertEquals("Order_400_17", v2Post(m.shop.team.manager, "/events/${m.shop.eventId}/refunds/${m.approved}/complete").andExpect { status { isBadRequest() } }.code())

            val completed = v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/refunds", mapOf("status" to "COMPLETED")).andExpect { status { isOk() } }.data()
            assertEquals(listOf(m.refused), completed.at("/content").map { it.at("/orderUuid").asText() })
            assertEquals(3, v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/refunds").andExpect { status { isOk() } }.data().at("/totalElements").asLong())
            v2Get(m.shop.team.outsider, "/events/${m.shop.eventId}/refunds").andExpect { status { isForbidden() } }
        }
    }

    @Nested
    @DisplayName("D-1 대시보드")
    inner class Dashboard {

        @Test
        fun `주문 건수·티켓별 판매 매수(무제한 null)·판매금액·입장 현황`() {
            val m = Mixed()
            val unlimited = createTicket(m.shop.team.manager, m.shop.eventId, freeBody(name = "무제한", supplyCount = null, overrides = mapOf("isQuantityPublic" to false)))
            v1Buy(newBuyer(), m.shop.team.master, m.shop.eventId, unlimited, approval = false)
            val ticket = m.shop.ticketUuids(m.approved).first()
            checkIn(m.shop.team.guest, m.shop.eventId, ticket).andExpect { status { isOk() } }

            val data = v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/dashboard").andExpect { status { isOk() } }.data()
            assertEquals(1, data.at("/orders/pendingApprove").asLong())
            assertEquals(2, data.at("/orders/approved").asLong())
            assertEquals(2, data.at("/orders/refused").asLong())
            assertEquals(3, data.at("/orders/refundRequested").asLong())
            // 두둥 2장(승인) + 무제한 1장 (취소분은 재고 복구)
            assertEquals(3, data.at("/tickets/totalSoldCount").asLong())
            assertTrue(data.at("/tickets/totalSupplyCount").isNull)
            val items = data.at("/tickets/items").associateBy { it.at("/ticketItemId").asLong() }
            assertEquals(2, items.getValue(m.shop.ticketId).at("/soldCount").asLong())
            assertEquals(20, items.getValue(m.shop.ticketId).at("/supplyCount").asLong())
            assertTrue(items.getValue(unlimited).at("/supplyCount").isNull)
            assertEquals(14000, data.at("/salesAmount").asLong())
            assertEquals(3, data.at("/entrance/issuedCount").asLong())
            assertEquals(1, data.at("/entrance/enteredCount").asLong())
            assertEquals(2, data.at("/entrance/notEnteredCount").asLong())
            assertEquals(33.3, data.at("/entrance/entranceRate").asDouble())

            v2Get(m.shop.team.outsider, "/events/${m.shop.eventId}/dashboard").andExpect { status { isForbidden() } }
        }

        @Test
        fun `판매금액은 할인 후 결제금액 합 (쿠폰 할인 주문)`() {
            val shop = Shop()
            shop.approved(newBuyer()) // 7000
            // v1 쿠폰 주문은 PG(유료) 선착순 티켓 전용이라 v1 API 로 만들 수 없어, 결제 완료(CONFIRM) + 할인 1000원 주문을 직접 저장한다
            val coupon = Order.forTest(userId = newBuyer().id, orderName = "쿠폰주문", orderStatus = OrderStatus.CONFIRM, orderMethod = OrderMethod.PAYMENT, eventId = shop.eventId)
            ReflectionTestUtils.setField(coupon, "totalPaymentInfo", PaymentInfo.of(paymentAmount = Money.wons(4000), supplyAmount = Money.wons(5000), discountAmount = Money.wons(1000)))
            ReflectionTestUtils.setField(coupon, "approvedAt", LocalDateTime.now())
            orderRepository.save(coupon)
            val data = v2Get(shop.team.guest, "/events/${shop.eventId}/dashboard").andExpect { status { isOk() } }.data()
            assertEquals(11000, data.at("/salesAmount").asLong())
            assertEquals(2, data.at("/orders/approved").asLong())
        }
    }

    @Nested
    @DisplayName("R-6 엑셀")
    inner class Export {

        @Test
        fun `헤더·행 수가 필터와 같다`() {
            val m = Mixed()
            val all = v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/orders/export").andExpect {
                status { isOk() }
                header { string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") }
                header { string("Content-Disposition", "attachment; filename=\"orders-${m.shop.eventId}.xlsx\"") }
            }.sheet()
            // 이메일은 엑셀에 넣지 않는다 (주문 상세에서만)
            assertEquals(listOf("주문번호", "주문자", "연락처", "입금자명", "티켓", "매수", "결제금액", "주문일시", "상태", "환불", "거절·취소 사유"), all.headers())
            assertEquals(5, all.lastRowNum)
            assertEquals(setOf("승인 대기", "승인 완료", "승인 거절", "취소"), all.column("상태").toSet())
            assertTrue("010-3333-4444" in all.column("연락처"))

            val refused = v2Get(m.shop.team.guest, "/events/${m.shop.eventId}/orders/export", mapOf("status" to "REFUSED")).andExpect { status { isOk() } }.sheet()
            assertEquals(2, refused.lastRowNum)
            assertEquals(setOf("티켓 매진", "v1 사유"), refused.column("거절·취소 사유").toSet())
            assertEquals(setOf("환불 요청"), refused.column("환불").toSet())

            for (who in listOf(m.shop.team.guest, m.shop.team.manager, m.shop.team.master, superAdmin())) {
                v2Get(who, "/events/${m.shop.eventId}/orders/export").andExpect { status { isOk() } }
            }
            v2Get(m.shop.team.outsider, "/events/${m.shop.eventId}/orders/export").andExpect { status { isForbidden() } }
            v2Get(null, "/events/${m.shop.eventId}/orders/export").andExpect { status { isUnauthorized() } }
            v2Get(Shop("남의호스트").team.master, "/events/${m.shop.eventId}/orders/export").andExpect { status { isForbidden() } }
        }

        @Test
        fun `수식 인젝션 방어 - = + - @ 로 시작하는 사용자 입력은 앞에 작은따옴표`() {
            val shop = Shop()
            val evil = newBuyer("=HYPERLINK(\"x\")")
            val order = shop.order(evil)
            refuse(shop.team.manager, shop.eventId, order, "ETC", "+SUM(A1:A9)").andExpect { status { isOk() } }
            val sheet = v2Get(shop.team.guest, "/events/${shop.eventId}/orders/export").andExpect { status { isOk() } }.sheet()
            assertEquals(listOf("'=HYPERLINK(\"x\")"), sheet.column("주문자"))
            assertEquals(listOf("'+SUM(A1:A9)"), sheet.column("거절·취소 사유"))
            // 화면(JSON)에는 원문 그대로
            assertEquals("=HYPERLINK(\"x\")", orders(shop.team.guest, shop.eventId).at("/orders/content/0/buyerName").asText())
        }
    }

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 거절 사유가 v1 환불 목록 cancelReason 에 보이고, v1 어드민 주문 목록·사용자 주문 상세가 그대로 동작`() {
            val shop = Shop()
            val buyer = newBuyer()
            val order = shop.order(buyer)
            refuse(shop.team.manager, shop.eventId, order, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } }

            mockMvc.get("/api/v1/events/${shop.eventId}/refunds") { with(auth(shop.team.master)) }.andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].orderId") { value(order) }
                jsonPath("$.data.content[0].cancelReason") { value("입금 미확인") }
                jsonPath("$.data.content[0].refundStatus") { value("REFUND_REQUESTED") }
            }
            mockMvc.get("/api/v1/events/${shop.eventId}/orders") {
                with(auth(shop.team.master))
                param("orderStage", "CONFIRMED")
            }.andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].orderUuid") { value(order) }
                jsonPath("$.data.content[0].orderStatus") { value("취소된 결제") }
            }
            mockMvc.get("/api/v1/orders/$order") { with(auth(buyer)) }.andExpect {
                status { isOk() }
            }
            // v1 환불 완료 API 로 완료해도 v2 에서 완료로 보인다
            mockMvc.patch("/api/v1/events/${shop.eventId}/refunds/$order/complete") { with(auth(shop.team.master)) }.andExpect { status { isOk() } }
            assertEquals("COMPLETED", orders(shop.team.guest, shop.eventId).at("/orders/content/0/refundStatus").asText())
            assertNull(orderRepository.findByOrderUuid(order).get().approvedAt)
        }
    }
}
