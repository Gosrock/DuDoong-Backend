package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.order.dto.request.V2CreateOrderRequest
import band.gosrock.api.v2.order.usecase.V2CreateOrderUseCase
import band.gosrock.api.v2.order.usecase.V2ReadMyOrdersUseCase
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.domains.order.service.FreeOrderService
import band.gosrock.domain.domains.order.service.v2.V2UserOrderDomainService
import band.gosrock.domain.domains.ticket_item.exception.TicketItemQuantityLackException
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.web.servlet.patch
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.issuedTicket.domain.IssuedTicketStatus
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.domain.TicketType
import band.gosrock.domain.domains.user.domain.User
import java.time.LocalDateTime
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

/** v2 사용자 앱 주문 통합 테스트 (#718): O-1 ~ O-4 + 호스트 환불 계좌 노출 범위 + v1 호환 + 동시성 + 알림 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 사용자 앱 - 주문")
class V2UserOrderControllerTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var v2UserOrderDomainService: V2UserOrderDomainService

    @Autowired private lateinit var readMyOrdersUseCase: V2ReadMyOrdersUseCase

    companion object {
        private const val STABLE_MS = 500L
    }

    private fun codeOf(buyer: User, body: Map<String, Any?>): String =
        v2CreateOrder(buyer, body).andExpect { status { isBadRequest() } }.code()

    @Nested
    @DisplayName("O-1 주문 생성")
    inner class Create {

        @Test
        fun `두둥티켓 계좌이체 - 승인 대기, 결제 방식·입금자명(앞뒤 공백 제거) 저장, 옵션 추가금 포함 금액, 계좌 제공`() {
            val shop = Shop()
            val buyer = newBuyer()
            val data = v2OrderOk(buyer, shopBody(shop, quantity = 2, depositorName = "  128구구 "))
            assertEquals("PENDING_APPROVE", data.at("/status").asText())
            assertEquals("BANK_TRANSFER", data.at("/paymentChannel").asText())
            assertEquals("128구구", data.at("/depositorName").asText())
            assertEquals(2, data.at("/quantity").asLong())
            assertEquals(12000, data.at("/payment/ticketAmount").asLong())
            assertEquals(2000, data.at("/payment/optionAmount").asLong())
            assertEquals(14000, data.at("/payment/totalAmount").asLong())
            assertEquals("신한은행", data.at("/payment/account/bankName").asText())
            assertEquals("고스락", data.at("/payment/account/accountHolder").asText())
            assertEquals("110-123-456789", data.at("/payment/account/accountNumber").asText())
            assertEquals(1, data.at("/lines").size())
            assertEquals(listOf("예", "홍길동"), data.at("/lines/0/optionAnswers").map { it.at("/answer").asText() })
            assertTrue(data.at("/canCancel").asBoolean())
            assertTrue(data.at("/orderNo").asText().startsWith("R"))
            assertEquals("DUDOONG", data.at("/ticket/payType").asText())

            val saved = orderRepository.findByOrderUuid(data.at("/orderUuid").asText()).get()
            assertEquals(OrderPaymentChannel.BANK_TRANSFER, saved.paymentChannel)
            assertEquals(Money.wons(14000), saved.getTotalPaymentPrice())
            // 호스트 v2 주문 목록에도 입금자명·결제 방식이 보인다
            val element = orders(shop.team.guest, shop.eventId).at("/orders/content/0")
            assertEquals("128구구", element.at("/depositorName").asText())
            assertEquals("BANK_TRANSFER", element.at("/paymentChannel").asText())
        }

        @Test
        fun `토스 송금 - TOSS_TRANSFER 로 기록, 승인 대기`() {
            val shop = Shop()
            val data = v2OrderOk(newBuyer(), shopBody(shop, method = "TOSS_TRANSFER", yes = false))
            assertEquals("TOSS_TRANSFER", data.at("/paymentChannel").asText())
            assertEquals("PENDING_APPROVE", data.at("/status").asText())
            assertEquals(6000, data.at("/payment/totalAmount").asLong())
            assertEquals("아니요", data.at("/lines/0/optionAnswers/0/answer").asText())
        }

        @Test
        fun `무료 승인 OFF - 즉시 발급(APPROVED), 금액·계좌 없음, 재고 감소`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false, supplyCount = 5)
            val buyer = newBuyer()
            val data = v2OrderOk(buyer, freeBodyOf(shop, free, quantity = 2))
            assertEquals("APPROVED", data.at("/status").asText())
            assertEquals("FREE", data.at("/paymentChannel").asText())
            assertTrue(data.at("/payment").isNull)
            assertTrue(data.at("/depositorName").isNull)
            assertEquals(2, data.at("/issuedTickets").size())
            assertEquals(3, stock(free))
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(data.at("/orderUuid").asText()).get().orderStatus)
        }

        @Test
        fun `무료 승인 ON - 승인 대기, 무료는 입금자명 무시`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = true)
            val data = v2OrderOk(newBuyer(), orderBody(shop.eventId, free, method = "FREE", depositorName = "무시"))
            assertEquals("PENDING_APPROVE", data.at("/status").asText())
            assertTrue(data.at("/depositorName").isNull)
            assertEquals(0, data.at("/issuedTickets").size())
        }

        @Test
        fun `티켓별 옵션 - 수량 1 라인 N개, 승인하면 발급 티켓마다 자기 답변, 금액은 라인 합`() {
            val shop = Shop()
            val buyer = newBuyer()
            val body = orderBody(
                shop.eventId, shop.ticketId, quantity = 2,
                perTicket = listOf(answers(shop, yes = true, text = "첫째"), answers(shop, yes = false, text = "둘째")),
            )
            val data = v2OrderOk(buyer, body)
            val uuid = data.at("/orderUuid").asText()
            assertEquals(2, data.at("/lines").size())
            assertEquals(listOf(1L, 1L), data.at("/lines").map { it.at("/quantity").asLong() })
            assertEquals(13000, data.at("/payment/totalAmount").asLong())
            assertEquals(1000, data.at("/payment/optionAmount").asLong())

            v1Approve(shop.team.master, shop.eventId, uuid).andExpect { status { isOk() } }
            val detail = myOrder(buyer, uuid).andExpect { status { isOk() } }.data()
            val ticketAnswers = detail.at("/issuedTickets").map { t -> t.at("/optionAnswers").map { it.at("/answer").asText() } }.toSet()
            assertEquals(setOf(listOf("예", "첫째"), listOf("아니요", "둘째")), ticketAnswers)
            // 호스트 v2 상세도 같은 구조
            val host = hostDetail(shop.team.guest, shop.eventId, uuid)
            assertEquals(2, host.at("/lines").size())
            assertEquals(2, host.at("/issuedTickets").size())
            assertEquals(13000, host.at("/order/totalPaymentAmount").asLong())
            // 대시보드 판매금액 = 주문 결제금액 (라인 분할과 무관)
            assertEquals(13000, v2Get(shop.team.guest, "/events/${shop.eventId}/dashboard").andExpect { status { isOk() } }.data().at("/salesAmount").asLong())
        }

        @Test
        fun `티켓별 옵션 개수가 수량과 다르면 Order_400_23`() {
            val shop = Shop()
            val body = orderBody(shop.eventId, shop.ticketId, quantity = 2, perTicket = listOf(answers(shop)))
            assertEquals("Order_400_23", codeOf(newBuyer(), body))
        }

        @Test
        fun `옵션 답변 오류 - 누락·중복·다른 티켓 옵션·잘못된 네아니오·빈 주관식은 Order_400_23`() {
            val shop = Shop()
            val buyer = newBuyer()
            val other = Shop()
            val bad = listOf(
                listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "YES")),
                answers(shop) + listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "NO")),
                listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "YES"), mapOf("optionId" to other.subjectiveOptionId, "answer" to "x")),
                listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "MAYBE"), mapOf("optionId" to shop.subjectiveOptionId, "answer" to "x")),
                listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "YES"), mapOf("optionId" to shop.subjectiveOptionId, "answer" to "   ")),
                emptyList(),
            )
            bad.forEachIndexed { i, a ->
                assertEquals("Order_400_23", codeOf(buyer, orderBody(shop.eventId, shop.ticketId, answers = a)), "case $i")
            }
            assertEquals(0, myOrders(buyer).at("/content").size())
        }

        @Test
        fun `네아니오 별칭 - 예·네, 아니요·아니오 도 받는다 (저장은 v1 값 예·아니요)`() {
            val shop = Shop()
            listOf("네" to "예", "예" to "예", "아니오" to "아니요", "no" to "아니요").forEach { (input, stored) ->
                val a = listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to input), mapOf("optionId" to shop.subjectiveOptionId, "answer" to "a"))
                val data = v2OrderOk(newBuyer(), orderBody(shop.eventId, shop.ticketId, answers = a))
                assertEquals(stored, data.at("/lines/0/optionAnswers/0/answer").asText(), input)
            }
        }

        @Test
        fun `결제 방식 불일치 - 두둥티켓에 FREE, 무료에 계좌이체는 Order_400_21`() {
            val shop = Shop()
            assertEquals("Order_400_21", codeOf(newBuyer(), shopBody(shop, method = "FREE")))
            val free = freeTicket(shop, approvalRequired = false)
            assertEquals("Order_400_21", codeOf(newBuyer(), orderBody(shop.eventId, free, method = "BANK_TRANSFER")))
        }

        @Test
        fun `입금자명 - 없음·공백·21자는 Order_400_22, 20자는 성공`() {
            val shop = Shop()
            val buyer = newBuyer()
            listOf(null, "   ", "가".repeat(21)).forEach {
                assertEquals("Order_400_22", codeOf(buyer, shopBody(shop, depositorName = it)), "$it")
            }
            v2OrderOk(buyer, shopBody(shop, depositorName = "가".repeat(20)))
        }

        @Test
        fun `환불 규정 동의가 false 이거나 없으면 400`() {
            val shop = Shop()
            listOf(false, null).forEach {
                v2CreateOrder(newBuyer(), shopBody(shop) + mapOf("agreeRefundPolicy" to it)).andExpect { status { isBadRequest() } }
            }
        }

        @Test
        fun `PG(PRICE) 티켓은 Order_400_20, 다른 공연 티켓은 404`() {
            val shop = Shop()
            val pg = ticketItemRepository.save(
                TicketItem(
                    payType = TicketPayType.PRICE_TICKET, name = "카드", description = "v1", price = Money.wons(5000),
                    quantity = 10, supplyCount = 10, purchaseLimit = 2, type = TicketType.FIRST_COME_FIRST_SERVED,
                    isQuantityPublic = true, isSellable = true, eventId = shop.eventId,
                ),
            )
            assertEquals("Order_400_20", codeOf(newBuyer(), orderBody(shop.eventId, pg.id!!, method = "BANK_TRANSFER")))
            val other = Shop()
            v2CreateOrder(newBuyer(), orderBody(other.eventId, shop.ticketId, answers = answers(shop))).andExpect { status { isNotFound() } }
        }

        @Test
        fun `판매 불가 - 판매 중단·판매 기간 밖·매진·1인 제한 초과·공연 시작 후·준비중 공연은 400`() {
            val buyer = newBuyer()

            val suspended = Shop()
            suspend(suspended.team.manager, suspended.eventId, suspended.ticketId).andExpect { status { isOk() } }
            assertEquals("Ticket_Item_400_10", codeOf(buyer, shopBody(suspended)))

            val period = Shop()
            val item = ticketItemRepository.findById(period.ticketId).get()
            ReflectionTestUtils.setField(item, "saleStartAt", LocalDateTime.now().plusDays(1))
            ticketItemRepository.save(item)
            assertEquals("Ticket_Item_400_10", codeOf(buyer, shopBody(period)))

            val soldOut = Shop()
            val soldItem = ticketItemRepository.findById(soldOut.ticketId).get()
            ReflectionTestUtils.setField(soldItem, "quantity", 1L)
            ticketItemRepository.save(soldItem)
            assertEquals("Ticket_Item_400_1", codeOf(buyer, shopBody(soldOut, quantity = 2)))

            val limit = Shop()
            assertEquals("Ticket_Item_400_6", codeOf(buyer, shopBody(limit, quantity = 5)))
            // 승인 대기 2장 + 3장 = 5 > 4 (v1 승인 대기 1인 제한)
            v2OrderOk(buyer, shopBody(limit, quantity = 2))
            assertEquals("Order_400_15", codeOf(buyer, shopBody(limit, quantity = 3)))

            val started = Shop()
            setEventStart(started.eventId, LocalDateTime.now().minusMinutes(1))
            assertEquals("Event_400_6", codeOf(buyer, shopBody(started)))

            val preparing = Shop()
            setEventStatus(preparing.eventId, EventStatus.PREPARING)
            assertEquals("Event_400_5", codeOf(buyer, shopBody(preparing)))
        }

        @Test
        fun `수량 경계 - 0·음수·최대(100) 초과는 400, 주문 미생성, 100 은 요청 검증 통과`() {
            val shop = Shop()
            val buyer = newBuyer()
            listOf(0L, -1L, 101L).forEach { q ->
                v2CreateOrder(buyer, shopBody(shop, quantity = q)).andExpect { status { isBadRequest() } }
            }
            assertEquals(0, orderRepository.findAll().count { it.userId == buyer.id })
            // 100 은 요청 검증은 통과하고 도메인 검사(재고 20장)에 걸린다
            assertEquals("Ticket_Item_400_1", codeOf(buyer, shopBody(shop, quantity = 100)))
        }

        @Test
        fun `중첩 옵션 검증 - optionId null·답변 300자 초과·null 원소는 400 (500 아님), 주문 미생성`() {
            val shop = Shop()
            val buyer = newBuyer()
            val nullId = listOf(mapOf("optionId" to null, "answer" to "YES"), mapOf("optionId" to shop.subjectiveOptionId, "answer" to "a"))
            val tooLong = listOf(mapOf("optionId" to shop.yesNoOptionId, "answer" to "YES"), mapOf("optionId" to shop.subjectiveOptionId, "answer" to "가".repeat(301)))
            val bodies = listOf(
                orderBody(shop.eventId, shop.ticketId, answers = nullId),
                orderBody(shop.eventId, shop.ticketId, answers = tooLong),
                orderBody(shop.eventId, shop.ticketId, quantity = 2, perTicket = listOf(answers(shop), nullId)),
                orderBody(shop.eventId, shop.ticketId, quantity = 2, perTicket = listOf(answers(shop), tooLong)),
                orderBody(shop.eventId, shop.ticketId, quantity = 2) + mapOf("options" to mapOf("applyToAll" to false), "perTicketOptions" to listOf(answers(shop), null)),
                orderBody(shop.eventId, shop.ticketId) + mapOf("options" to mapOf("applyToAll" to true, "answers" to listOf(null))),
            )
            bodies.forEachIndexed { i, body ->
                val r = v2CreateOrder(buyer, body).andReturn().response
                assertEquals(400, r.status, "case $i ${r.getContentAsString(Charsets.UTF_8)}")
            }
            assertEquals(0, orderRepository.findAll().count { it.userId == buyer.id })
        }

        @Test
        fun `입금자명은 주문 시점 값 - 닉네임을 바꿔도 기존 주문 입금자명 유지 (DEC-022 #6)`() {
            val shop = Shop()
            val buyer = newBuyer("원래닉")
            val uuid = v2OrderOk(buyer, shopBody(shop, depositorName = "원래닉")).at("/orderUuid").asText()
            mockMvc.patch("/api/v1/users/me/name") {
                with(auth(buyer))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("name" to "새닉네임"))
            }.andExpect { status { isOk() } }
            assertEquals("새닉네임", userRepository.findById(buyer.id!!).get().profile!!.name)
            assertEquals("원래닉", myOrder(buyer, uuid).andExpect { status { isOk() } }.data().at("/depositorName").asText())
            assertEquals("원래닉", hostDetail(shop.team.guest, shop.eventId, uuid).at("/order/depositorName").asText())
        }

        @Test
        fun `무료 확정이 실패하면 주문은 FAILED, 같은 요청 재시도는 새 주문으로 정상 진행`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false)
            val buyer = newBuyer()
            val failingConfirm = mock(FreeOrderService::class.java)
            `when`(failingConfirm.execute(anyString(), anyLong())).thenThrow(TicketItemQuantityLackException.EXCEPTION)
            val useCase = V2CreateOrderUseCase(v2UserOrderDomainService, failingConfirm, readMyOrdersUseCase)
            val request = objectMapper.convertValue(freeBodyOf(shop, free), V2CreateOrderRequest::class.java)
            assertThrows<DuDoongCodeException> { useCase.execute(buyer.id!!, request) }
            val failed = orderRepository.findAll().single { it.userId == buyer.id }
            assertEquals(OrderStatus.FAILED, failed.orderStatus)
            assertEquals(0, myOrders(buyer).at("/content").size())

            val retry = v2OrderOk(buyer, freeBodyOf(shop, free))
            assertEquals("APPROVED", retry.at("/status").asText())
            assertNotEquals(failed.uuid, retry.at("/orderUuid").asText())
        }

        @Test
        fun `승인(완료)되면 v1 과 같이 v1 장바구니가 지워진다 (DoneOrderEvent, v1 기존 동작 고정)`() {
            val shop = Shop()
            val buyer = newBuyer()
            v1Cart(buyer, shop.ticketId, 1, v1Answers(buyer, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            assertTrue(cartRepository.findByUserId(buyer.id!!).isPresent, "주문 생성만으로는 유지")
            v1Approve(shop.team.master, shop.eventId, uuid).andExpect { status { isOk() } }
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && cartRepository.findByUserId(buyer.id!!).isPresent) Thread.sleep(50)
            assertFalse(cartRepository.findByUserId(buyer.id!!).isPresent)
        }

        @Test
        fun `비로그인은 401`() {
            val shop = Shop()
            v2CreateOrder(null, shopBody(shop)).andExpect { status { isUnauthorized() } }
            v2Get(null, "/me/orders").andExpect { status { isUnauthorized() } }
            v2Get(null, "/me/orders/abc").andExpect { status { isUnauthorized() } }
            cancelMy(null, "abc").andExpect { status { isUnauthorized() } }
        }

        @Test
        fun `같은 요청이 10초 안에 다시 오면 앞 주문을 돌려준다 (중복 생성 없음), 내용이 다르면 새 주문`() {
            val shop = Shop()
            val buyer = newBuyer()
            val first = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            val again = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            assertEquals(first, again)
            val different = v2OrderOk(buyer, shopBody(shop, quantity = 2)).at("/orderUuid").asText()
            assertNotEquals(first, different)
            assertNotEquals(first, v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText(), "옵션 답변이 다르면 새 주문")
            assertEquals(3, myOrders(buyer).at("/content").size())
            // 거절된 주문은 중복으로 보지 않는다 (다시 주문)
            refuse(shop.team.manager, shop.eventId, first, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } }
            v1Approve(shop.team.master, shop.eventId, different).andExpect { status { isOk() } }
            // 1인 제한: 발급 2 + 승인 대기 1 + 새 1 = 4 이하
            assertNotEquals(first, v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText())
        }

        @Test
        fun `v1 장바구니(최근 장바구니)를 덮어쓰지 않는다`() {
            val shop = Shop()
            val buyer = newBuyer()
            val cartId = v1Cart(buyer, shop.ticketId, 1, v1Answers(buyer, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }.data().at("/cartId").asLong()
            v2OrderOk(buyer, shopBody(shop))
            assertEquals(cartId, cartRepository.findByUserId(buyer.id!!).get().id)
        }
    }

    @Nested
    @DisplayName("O-2 내 주문 목록")
    inner class ListMine {

        @Test
        fun `상태 필터·최신 순·페이징, 남의 주문 없음, v1 주문 섞임(결제 진행 중 v1 주문은 제외)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val pending = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            val approved = v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText()
                .also { v1Approve(shop.team.master, shop.eventId, it).andExpect { status { isOk() } } }
            val refused = shop.order(buyer).also { refuse(shop.team.manager, shop.eventId, it, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } } }
            val free = freeTicket(shop, approvalRequired = false)
            val freeCanceled = v2OrderOk(buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
                .also { cancelMy(buyer, it).andExpect { status { isOk() } } }
            val refunded = v2OrderOk(buyer, shopBody(shop, depositorName = "환불")).at("/orderUuid").asText()
                .also { cancelMy(buyer, it, refundAccount).andExpect { status { isOk() } } }
            // v1 무료 선착순 주문(확정 전 PENDING_PAYMENT)은 목록 제외
            val v1Free = saveV1FreeTicket(shop.eventId)
            v1Order(buyer, shop.eventId, v1Free.id!!)
            v2OrderOk(newBuyer(), shopBody(shop))

            val all = myOrders(buyer)
            val uuids = all.at("/content").map { it.at("/orderUuid").asText() }
            assertEquals(listOf(refunded, freeCanceled, refused, approved, pending), uuids)
            assertEquals(5, all.at("/totalElements").asLong())
            val first = all.at("/content/0")
            assertEquals(shop.eventId, first.at("/event/eventId").asLong())
            assertEquals("정기공연", first.at("/event/name").asText())
            assertEquals("UPCOMING", first.at("/event/displayStatus").asText())
            assertEquals("REFUNDED", first.at("/status").asText())
            assertEquals("REQUESTED", first.at("/refundStatus").asText())
            assertEquals(7000, first.at("/totalAmount").asLong())

            fun only(status: String) = myOrders(buyer, mapOf("status" to status)).at("/content").map { it.at("/orderUuid").asText() }
            assertEquals(listOf(pending), only("PENDING_APPROVE"))
            assertEquals(listOf(approved), only("APPROVED"))
            assertEquals(listOf(refused), only("REFUSED"))
            assertEquals(listOf(freeCanceled), only("CANCELED"))
            assertEquals(listOf(refunded), only("REFUNDED"))

            val page = myOrders(buyer, mapOf("page" to "1", "size" to "2"))
            assertEquals(listOf(refused, approved), page.at("/content").map { it.at("/orderUuid").asText() })
            assertEquals(3, page.at("/totalPages").asInt())
            assertTrue(page.at("/hasNext").asBoolean())
        }
    }

    @Nested
    @DisplayName("O-3 내 주문 상세")
    inner class Detail {

        @Test
        fun `남의 주문·없는 주문은 404`() {
            val shop = Shop()
            val uuid = v2OrderOk(newBuyer(), shopBody(shop)).at("/orderUuid").asText()
            myOrder(newBuyer(), uuid).andExpect { status { isNotFound() } }
            myOrder(shop.team.master, uuid).andExpect { status { isNotFound() } }
            myOrder(newBuyer(), "nope").andExpect { status { isNotFound() } }
        }

        @Test
        fun `거절 사유 - v2 거절은 종류 + 문구, v1 거절은 문구만`() {
            val shop = Shop()
            val buyer = newBuyer()
            val v2 = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, v2, "ETC", "중복 입금").andExpect { status { isOk() } }
            val d = myOrder(buyer, v2).andExpect { status { isOk() } }.data()
            assertEquals("REFUSED", d.at("/status").asText())
            assertEquals("ETC", d.at("/refuseReasonType").asText())
            assertEquals("중복 입금", d.at("/refuseReason").asText())
            assertFalse(d.at("/canCancel").asBoolean())

            val v1 = v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText()
            mockMvc.post("/api/v1/events/${shop.eventId}/orders/$v1/refuse") {
                with(auth(shop.team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("reason" to "v1 사유"))
            }.andExpect { status { isOk() } }
            val d1 = myOrder(buyer, v1).andExpect { status { isOk() } }.data()
            assertEquals("REFUSED", d1.at("/status").asText())
            assertTrue(d1.at("/refuseReasonType").isNull)
            assertEquals("v1 사유", d1.at("/refuseReason").asText())
        }

        @Test
        fun `v1 으로 만든 주문도 조회 - 결제 방식 null, 두둥 계좌 제공`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = shop.order(buyer)
            val d = myOrder(buyer, uuid).andExpect { status { isOk() } }.data()
            assertTrue(d.at("/paymentChannel").isNull)
            assertEquals("PENDING_APPROVE", d.at("/status").asText())
            assertEquals(7000, d.at("/payment/totalAmount").asLong())
            assertEquals("신한은행", d.at("/payment/account/bankName").asText())
        }
    }

    @Nested
    @DisplayName("O-4 취소·환불 요청")
    inner class Cancel {

        @Test
        fun `승인 대기 유료 - 환불 계좌 필수(Order_400_25), 취소하면 REFUND + 환불 요청, 계좌는 본인에게 뒤 4자리만`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            assertEquals("Order_400_25", cancelMy(buyer, uuid).andExpect { status { isBadRequest() } }.code())
            cancelMy(buyer, uuid, refundAccount + mapOf("accountNumber" to "abc")).andExpect { status { isBadRequest() } }

            val d = cancelMy(buyer, uuid, refundAccount).andExpect { status { isOk() } }.data()
            assertEquals("REFUNDED", d.at("/status").asText())
            assertEquals("REQUESTED", d.at("/refundStatus").asText())
            assertEquals("국민은행", d.at("/refundAccount/bankName").asText())
            assertEquals("*********8901", d.at("/refundAccount/maskedAccountNumber").asText())
            assertFalse(d.at("/canCancel").asBoolean())
            assertNotNull(d.at("/withdrawnAt").textValue())

            val saved = orderRepository.findByOrderUuid(uuid).get()
            assertEquals(OrderStatus.REFUND, saved.orderStatus)
            assertEquals(RefundStatus.REFUND_REQUESTED, saved.refundStatus)
            assertNull(saved.approvedAt)
            assertEquals("123-45-678901", refundAccountRepository.findByOrderId(saved.id!!)!!.accountNumber)
            // 호스트: 취소(CANCELED) 분류, 거절 아님
            val host = hostDetail(shop.team.manager, shop.eventId, uuid)
            assertEquals("CANCELED", host.at("/order/status").asText())
            assertEquals("REQUESTED", host.at("/order/refundStatus").asText())
        }

        @Test
        fun `이미 취소한 주문을 다시 취소하면 Order_400_5`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, uuid, refundAccount).andExpect { status { isOk() } }
            assertEquals("Order_400_5", cancelMy(buyer, uuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertEquals(1, refundAccountRepository.findAll().count { it.orderId == orderRepository.findByOrderUuid(uuid).get().id })
        }

        @Test
        fun `승인 완료 유료 - v1 사용자 환불과 같은 전이 (발급 티켓 취소, 재고 복구, 환불 요청)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop, quantity = 2)).at("/orderUuid").asText()
            v1Approve(shop.team.master, shop.eventId, uuid).andExpect { status { isOk() } }
            assertEquals(18, stock(shop.ticketId))
            val d = cancelMy(buyer, uuid, refundAccount).andExpect { status { isOk() } }.data()
            assertEquals("REFUNDED", d.at("/status").asText())
            assertEquals(listOf("CANCELED", "CANCELED"), d.at("/issuedTickets").map { it.at("/entrance").asText() })
            assertEquals(20, stock(shop.ticketId))
            assertNotNull(orderRepository.findByOrderUuid(uuid).get().approvedAt)
        }

        @Test
        fun `무료 승인 주문 - 환불 계좌 없이 취소, 환불 요청 없음(CANCELED), 재고 복구`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false, supplyCount = 3)
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            assertEquals(2, stock(free))
            val d = cancelMy(buyer, uuid).andExpect { status { isOk() } }.data()
            assertEquals("CANCELED", d.at("/status").asText())
            assertEquals("NONE", d.at("/refundStatus").asText())
            assertTrue(d.at("/refundAccount").isNull)
            assertEquals(3, stock(free))
            assertEquals(RefundStatus.NONE, orderRepository.findByOrderUuid(uuid).get().refundStatus)
        }

        @Test
        fun `입장한 티켓이 있으면 Order_400_24, 공연 시작 후는 Order_400_9, 남의 주문은 404`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop, quantity = 2)).at("/orderUuid").asText()
            v1Approve(shop.team.master, shop.eventId, uuid).andExpect { status { isOk() } }
            checkIn(shop.team.guest, shop.eventId, shop.ticketUuids(uuid).first()).andExpect { status { isOk() } }
            assertFalse(myOrder(buyer, uuid).andExpect { status { isOk() } }.data().at("/canCancel").asBoolean())
            assertEquals("Order_400_24", cancelMy(buyer, uuid, refundAccount).andExpect { status { isBadRequest() } }.code())

            val pending = v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText()
            cancelMy(newBuyer(), pending, refundAccount).andExpect { status { isNotFound() } }
            setEventStart(shop.eventId, LocalDateTime.now().minusMinutes(1))
            assertFalse(myOrder(buyer, pending).andExpect { status { isOk() } }.data().at("/canCancel").asBoolean())
            assertEquals("Order_400_9", cancelMy(buyer, pending, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertEquals(OrderStatus.PENDING_APPROVE, orderRepository.findByOrderUuid(pending).get().orderStatus)
        }

        @Test
        fun `v1 카드(PG) 결제 완료 주문은 O-4 로 취소할 수 없다 (Order_400_24), 상태 유지`() {
            val shop = Shop()
            val buyer = newBuyer()
            val pg = ticketItemRepository.save(
                TicketItem(
                    payType = TicketPayType.PRICE_TICKET, name = "카드", description = "v1", price = Money.wons(5000),
                    quantity = 10, supplyCount = 10, purchaseLimit = 2, type = TicketType.FIRST_COME_FIRST_SERVED,
                    isQuantityPublic = true, isSellable = true, eventId = shop.eventId,
                ),
            )
            val uuid = v1Order(buyer, shop.eventId, pg.id!!)
            val order = orderRepository.findByOrderUuid(uuid).get()
            ReflectionTestUtils.setField(order, "orderStatus", OrderStatus.CONFIRM)
            ReflectionTestUtils.setField(order, "approvedAt", LocalDateTime.now())
            orderRepository.save(order)
            assertFalse(myOrder(buyer, uuid).andExpect { status { isOk() } }.data().at("/canCancel").asBoolean())
            assertEquals("Order_400_24", cancelMy(buyer, uuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertEquals(OrderStatus.CONFIRM, orderRepository.findByOrderUuid(uuid).get().orderStatus)
        }

        @Test
        fun `환불 계좌번호는 공백을 지워 저장`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, uuid, refundAccount + mapOf("accountNumber" to "123 45 678901")).andExpect { status { isOk() } }
            assertEquals("12345678901", refundAccountRepository.findByOrderId(orderRepository.findByOrderUuid(uuid).get().id!!)!!.accountNumber)
        }

        @Test
        fun `주문자 소유가 아닌 발급 티켓(선물 대비)이 있으면 Order_400_24`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            v1Approve(shop.team.master, shop.eventId, uuid).andExpect { status { isOk() } }
            val ticket = issuedTicketRepository.findAllByOrderUuid(uuid).first()
            ReflectionTestUtils.setField(ticket.userInfo!!, "userId", newBuyer().id)
            issuedTicketRepository.save(ticket)
            assertEquals("Order_400_24", cancelMy(buyer, uuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertEquals(0, myOrder(buyer, uuid).andExpect { status { isOk() } }.data().at("/issuedTickets").size())
        }
    }

    @Nested
    @DisplayName("호스트 환불 계좌 노출 범위")
    inner class HostRefundAccount {

        @Test
        fun `마스터·매니저만 주문 상세(R-2)·환불 목록(F-1)에서 환불 계좌 전체, 일반 멤버는 null`() {
            val shop = Shop()
            val buyer = newBuyer()
            val uuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, uuid, refundAccount).andExpect { status { isOk() } }

            listOf(shop.team.master, shop.team.manager).forEach {
                assertEquals("123-45-678901", hostDetail(it, shop.eventId, uuid).at("/refundAccount/accountNumber").asText())
                val refunds = v2Get(it, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data()
                assertEquals("홍길동", refunds.at("/content/0/refundAccount/accountHolder").asText())
            }
            assertTrue(hostDetail(shop.team.guest, shop.eventId, uuid).at("/refundAccount").isNull)
            val guestRefunds = v2Get(shop.team.guest, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data()
            assertEquals(uuid, guestRefunds.at("/content/0/orderUuid").asText())
            assertTrue(guestRefunds.at("/content/0/refundAccount").isNull)
            v2Get(shop.team.outsider, "/events/${shop.eventId}/orders/$uuid").andExpect { status { isForbidden() } }
            assertEquals("123-45-678901", hostDetail(superAdmin(), shop.eventId, uuid).at("/refundAccount/accountNumber").asText())

            // 환불 완료(F-2) 응답에도 계좌
            val done = v2Post(shop.team.manager, "/events/${shop.eventId}/refunds/$uuid/complete").andExpect { status { isOk() } }.data()
            assertEquals("COMPLETED", done.at("/order/refundStatus").asText())
            assertEquals("국민은행", done.at("/refundAccount/bankName").asText())
            assertEquals("COMPLETED", myOrder(buyer, uuid).andExpect { status { isOk() } }.data().at("/refundStatus").asText())
        }
    }

    @Nested
    @DisplayName("v1 호환")
    inner class V1Compat {

        @Test
        fun `v2 주문을 v1 호스트 API 로 승인·거절, v1 사용자 앱 조회·목록 정상`() {
            val shop = Shop()
            val buyer = newBuyer()
            val a = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            val b = v2OrderOk(buyer, shopBody(shop, yes = false)).at("/orderUuid").asText()
            v1Approve(shop.team.master, shop.eventId, a).andExpect { status { isOk() } }
            mockMvc.post("/api/v1/events/${shop.eventId}/orders/$b/refuse") {
                with(auth(shop.team.master))
                contentType = MediaType.APPLICATION_JSON
                content = json(mapOf("reason" to "입금 없음"))
            }.andExpect { status { isOk() } }

            val v1Detail = mockMvc.get("/api/v1/orders/$a") { with(auth(buyer)) }.andExpect { status { isOk() } }.data()
            assertEquals(a, v1Detail.at("/orderUuid").asText())
            assertEquals(1, issuedTicketRepository.findAllByOrderUuid(a).size)
            val v1List = mockMvc.get("/api/v1/orders") {
                with(auth(buyer))
                param("showing", "true")
            }.andExpect { status { isOk() } }.data()
            assertEquals(2, v1List.at("/content").size())
            // v1 어드민 주문 목록
            val v1Admin = mockMvc.get("/api/v1/events/${shop.eventId}/orders") {
                with(auth(shop.team.master))
                param("orderStage", "CONFIRMED")
            }.andExpect { status { isOk() } }.data()
            assertTrue(v1Admin.at("/content").any { it.at("/orderUuid").asText() == a }, v1Admin.toString())
            assertEquals("APPROVED", myOrder(buyer, a).andExpect { status { isOk() } }.data().at("/status").asText())
            assertEquals("REFUSED", myOrder(buyer, b).andExpect { status { isOk() } }.data().at("/status").asText())
        }
    }

    @Nested
    @DisplayName("동시성")
    inner class Concurrency {

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

        /**
         * 무료 선착순 '마지막 1장' 경합은 MySQL E2E(test_45)에서 검증한다: v1 무료 확정은 발급(별도 트랜잭션 커밋) 뒤 재고를 다시 검사하는데,
         * H2(READ COMMITTED)는 방금 커밋된 감소분을 보고 마지막 1장도 실패시킨다 (MySQL 은 트랜잭션 스냅샷이라 통과 — v1 과 같은 동작).
         * 여기서는 여러 사용자 동시 무료 주문이 락 순서(티켓관리 → 주문 → 티켓관리 재진입)로 막히거나 이중 발급되지 않는지만 본다
         */
        @Test
        fun `무료 선착순 동시 주문 - 모두 발급, 이중 발급 없음, 재고 정확`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false, supplyCount = 10)
            val buyers = (1..4).map { newBuyer("동시$it") }
            val results = concurrently(4) { i ->
                v2CreateOrder(buyers[i], freeBodyOf(shop, free)).andReturn().response.let { if (it.status == 200) "200" else it.getContentAsString(Charsets.UTF_8) }
            }
            assertEquals(List(4) { "200" }, results, "500 등 실패 없음")
            assertEquals(6, stock(free))
            buyers.forEach { b ->
                val uuid = myOrders(b).at("/content/0/orderUuid").asText()
                assertEquals(1, issuedTicketRepository.findAllByOrderUuid(uuid).size)
            }
        }

        /**
         * 무료 선착순 1인 제한 동시 우회 방지: 같은 사용자가 옵션 답변만 다른 요청(중복 판정 안 됨)을 동시에 보내도 발급이 제한(2장)을 넘지 않는다.
         * 성공 건수는 H2 격리 수준 특성(V2OperationTestSupport 참고)으로 MySQL 과 다를 수 있어 상한·실패 코드만 본다 (정확한 값은 MySQL E2E test_45)
         */
        @Test
        fun `무료 선착순 같은 사용자 동시 주문(옵션만 다름) - 1인 제한을 넘겨 발급되지 않음, 실패는 1인 제한 400`() {
            val shop = Shop()
            val free = createTicket(shop.team.manager, shop.eventId, freeBody(name = "제한", supplyCount = 10, approvalRequired = false, overrides = mapOf("purchaseLimit" to 2)))
            putOptions(shop.team.manager, shop.eventId, free, listOf(shop.subjectiveOptionId)).andExpect { status { isOk() } }
            val buyer = newBuyer()
            val results = concurrently(3) { i ->
                val body = orderBody(shop.eventId, free, answers = listOf(mapOf("optionId" to shop.subjectiveOptionId, "answer" to "답$i")), method = "FREE", depositorName = null)
                v2CreateOrder(buyer, body).andReturn().response.let { if (it.status == 200) "200" else it.getContentAsString(Charsets.UTF_8) }
            }
            assertTrue(results.count { it == "200" } >= 1, "$results")
            assertTrue(results.filter { it != "200" }.all { objectMapper.readTree(it).at("/code").asText() == "Ticket_Item_400_6" }, "$results")
            val issued = orderRepository.findAll().filter { it.userId == buyer.id }.sumOf { o ->
                issuedTicketRepository.findAllByOrderUuid(o.uuid!!).count { it.issuedTicketStatus != IssuedTicketStatus.CANCELED }
            }
            assertTrue(issued <= 2, "issued=$issued $results")
            assertEquals(results.count { it == "200" }, issued)
        }

        @Test
        fun `마지막 재고 1장에 두둥티켓 동시 주문 - 승인 대기는 1건만 (티켓 락 + v1 승인 대기 재고 검사)`() {
            val shop = Shop()
            val item = ticketItemRepository.findById(shop.ticketId).get()
            ReflectionTestUtils.setField(item, "quantity", 1L)
            ticketItemRepository.save(item)
            val buyers = (1..3).map { newBuyer("동시$it") }
            val results = concurrently(3) { i ->
                v2CreateOrder(buyers[i], shopBody(shop)).andReturn().response.let { if (it.status == 200) "200" else it.getContentAsString(Charsets.UTF_8) }
            }
            assertEquals(1, results.count { it == "200" }, "$results")
            // 패자는 재고 부족 400 (500 없음)
            assertEquals(List(2) { "Ticket_Item_400_1" }, results.filter { it != "200" }.map { objectMapper.readTree(it).at("/code").asText() }, "$results")
            assertEquals(1, orders(shop.team.guest, shop.eventId).at("/counts/pendingApprove").asLong())
        }

        @Test
        fun `같은 사용자 중복 요청 동시 3번 - 주문 1건 (모두 같은 주문을 받는다)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val results = concurrently(3) {
                val r = v2CreateOrder(buyer, shopBody(shop)).andReturn().response
                if (r.status == 200) objectMapper.readTree(r.getContentAsString(Charsets.UTF_8)).at("/data/orderUuid").asText() else "ERR ${r.status}"
            }
            assertEquals(1, results.toSet().size, "$results")
            assertEquals(1, myOrders(buyer).at("/content").size())
        }
    }

    @Nested
    @DisplayName("알림")
    inner class Notifications {

        private fun count(user: User, type: NotificationType) = notificationRepository.findAllByUserId(user.id!!).count { it.type == type }

        /** 처음 보일 때까지 기다린 뒤 [STABLE_MS] 더 기다려 최종 개수를 다시 센다 (뒤늦은 중복 저장도 잡는다) */
        private fun await(user: User, type: NotificationType): Int {
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && count(user, type) == 0) Thread.sleep(50)
            Thread.sleep(STABLE_MS)
            return count(user, type)
        }

        @Test
        fun `사용자 환불 요청 → 마스터·매니저 ORDER_REFUND_REQUESTED, 무료 취소 → ORDER_CANCELED_BY_USER, 일반 멤버 없음`() {
            val shop = Shop()
            val buyer = newBuyer()
            val paid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, paid, refundAccount).andExpect { status { isOk() } }
            assertEquals(1, await(shop.team.master, NotificationType.ORDER_REFUND_REQUESTED))
            assertEquals(1, await(shop.team.manager, NotificationType.ORDER_REFUND_REQUESTED))
            val n = notificationRepository.findAllByUserId(shop.team.manager.id!!).first { it.type == NotificationType.ORDER_REFUND_REQUESTED }
            assertEquals(paid, n.targetId)

            val free = freeTicket(shop, approvalRequired = false)
            val freeOrder = v2OrderOk(buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            cancelMy(buyer, freeOrder).andExpect { status { isOk() } }
            assertEquals(1, await(shop.team.manager, NotificationType.ORDER_CANCELED_BY_USER))
            assertTrue(notificationRepository.findAllByUserId(shop.team.guest.id!!).none {
                it.type == NotificationType.ORDER_REFUND_REQUESTED || it.type == NotificationType.ORDER_CANCELED_BY_USER
            })
            // 승인 대기 알림(v2 주문 접수)도 기존대로: 유료 주문 1건만 승인형 (무료 선착순은 결제형)
            assertEquals(1, await(shop.team.manager, NotificationType.ORDER_PENDING_APPROVE))
            assertEquals(IssuedTicketStatus.CANCELED, issuedTicketRepository.findAllByOrderUuid(freeOrder).single().issuedTicketStatus)
        }
    }
}
