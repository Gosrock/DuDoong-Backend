package band.gosrock.api.order

import band.gosrock.api.order.model.dto.request.CreateOrderRequest
import band.gosrock.api.order.service.CreateOrderUseCase
import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.supports.LockConnectionProbe
import band.gosrock.api.supports.ThreadConnections
import band.gosrock.api.v2.order.V2UserOrderTestSupport
import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.service.CreateOrderService
import jakarta.persistence.EntityManagerFactory
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.orm.jpa.EntityManagerHolder
import org.springframework.test.context.event.ApplicationEvents
import org.springframework.test.context.event.RecordApplicationEvents
import org.springframework.transaction.support.TransactionSynchronizationManager

/**
 * v1·v2 주문 생성 락 통일·승인 재검사 순서 (#724). 결정적 경합(v1 ↔ v2 동시 주문)은 MySQL E2E test_53.
 * H2 는 READ COMMITTED 라 승인 재검사 순서 문제(발급 뒤 검사 → 방금 발급분을 한 번 더 셈)가 그대로 드러난다
 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@DisplayName("v1 주문 생성 락·승인 재검사 순서 (#724)")
class OrderLockUnifyTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var orderAdaptor: OrderAdaptor

    @Autowired private lateinit var events: ApplicationEvents

    @Autowired private lateinit var createOrderUseCase: CreateOrderUseCase

    @Autowired private lateinit var createOrderService: CreateOrderService

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    /** 이 티켓의 승인 대기 수량 합계 (모든 사용자) */
    private fun pendingQuantity(ticketId: Long): Long = orderAdaptor.sumPendingApproveQuantity(ticketId)

    @Nested
    @DisplayName("승인 재검사 순서")
    inner class ApproveRecheckOrder {

        @Test
        fun `v1 승인 - 남은 재고 절반을 넘는 주문도 승인된다 (검사가 발급보다 먼저 — 방금 발급분을 두 번 세지 않음)`() {
            val shop = Shop()
            val ticketId = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "소량", supplyCount = 3, overrides = mapOf("purchaseLimit" to 4)))
            val buyer = newBuyer()
            val orderUuid = v1Order(buyer, shop.eventId, ticketId, quantity = 2)
            v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
            assertEquals(2, issuedTicketRepository.findAllByOrderUuid(orderUuid).size)
            assertEquals(1L, ticketItemRepository.findById(ticketId).get().quantity)
        }

        @Test
        fun `v1 승인 - 1인 제한 절반을 넘는 주문(4장 중 3장)도 승인된다`() {
            val shop = Shop()
            val ticketId = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "제한", supplyCount = 20, overrides = mapOf("purchaseLimit" to 4)))
            val orderUuid = v1Order(newBuyer(), shop.eventId, ticketId, quantity = 3)
            v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
            assertEquals(3, issuedTicketRepository.findAllByOrderUuid(orderUuid).size)
        }

        @Test
        fun `v1 무료 확정 - 남은 재고 절반을 넘는 주문도 확정된다`() {
            val shop = Shop()
            val ticketId = freeTicket(shop, approvalRequired = false, supplyCount = 3, name = "무료소량")
            val buyer = newBuyer()
            val orderUuid = v1Order(buyer, shop.eventId, ticketId, quantity = 2)
            v1FreeConfirm(buyer, orderUuid).andExpect { status { isOk() } }
            assertEquals(OrderStatus.APPROVED, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
            assertEquals(1L, ticketItemRepository.findById(ticketId).get().quantity)
        }

        @Test
        fun `승인 검사 실패(탈퇴한 사용자) - 발급하지 않고 같은 에러 코드, 주문은 승인 대기 그대로 (호스트가 거절)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = shop.order(buyer)
            userRepository.findById(buyer.id!!).get().also { it.withDrawUser() }.let { userRepository.save(it) }
            val code = v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isBadRequest() } }.code()
            assertEquals("Order_400_14", code)
            // 예전에는 발급(DoneOrderEvent) 뒤 실패 → 롤백 후 비동기 실패 핸들러가 주문을 FAILED 로 바꿨다. 이제 발급 이벤트 자체가 없다
            assertEquals(0, events.stream(DoneOrderEvent::class.java).filter { it.orderUuid == orderUuid }.count())
            assertEquals(OrderStatus.PENDING_APPROVE, orderRepository.findByOrderUuid(orderUuid).get().orderStatus)
            assertTrue(issuedTicketRepository.findAllByOrderUuid(orderUuid).isEmpty())
        }
    }

    @Nested
    @DisplayName("주문 생성 락 (v1·v2 같은 티켓 락)")
    inner class CreateLock {

        @Test
        fun `같은 사용자 v1(3장)·v2(2장) 동시 주문, 1인 제한 4 - 승인 대기 합계가 제한을 넘지 않는다 (여러 회차)`() {
            repeat(4) { round ->
                val shop = Shop()
                val ticketId = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "동시$round", supplyCount = 50, overrides = mapOf("purchaseLimit" to 4)))
                val buyer = newBuyer()
                val cartId = v1Cart(buyer, ticketId, quantity = 3).andExpect { status { isOk() } }.data().at("/cartId").asLong()
                val start = CountDownLatch(1)
                val pool = Executors.newFixedThreadPool(2)
                val statuses = Collections.synchronizedList(mutableListOf<Int>())
                pool.submit { start.await(); statuses += v1CreateOrder(buyer, cartId).andReturn().response.status }
                pool.submit { start.await(); statuses += v2CreateOrder(buyer, orderBody(shop.eventId, ticketId, 2, depositorName = "동시$round")).andReturn().response.status }
                start.countDown()
                pool.shutdown()
                assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
                assertEquals(1, statuses.count { it == 200 }, "한쪽만 성공 (회차 $round, $statuses)")
                assertTrue(pendingQuantity(ticketId) <= 4, "승인 대기 합계 ${pendingQuantity(ticketId)} (회차 $round)")
            }
        }

        @Test
        fun `락 키 읽기는 커넥션을 쥐지 않는다 - open-in-view 처럼 EntityManager 가 묶여 있어도 최대 1 (락 트랜잭션), 호출 측 트랜잭션 없음`() {
            val shop = Shop()
            val buyer = newBuyer()
            val cartId = v1Cart(buyer, shop.ticketId, quantity = 1, answers = v1Answers(buyer, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }.data().at("/cartId").asLong()
            val em = entityManagerFactory.createEntityManager()
            TransactionSynchronizationManager.bindResource(entityManagerFactory, EntityManagerHolder(em))
            try {
                ThreadConnections.reset()
                createOrderUseCase.execute(buyer.id!!, CreateOrderRequest(cartId = cartId, couponId = null))
                assertTrue(ThreadConnections.peak <= 1, "최대 ${ThreadConnections.peak} — 락 키를 요청 EntityManager(JPA)로 읽으면 그 커넥션을 쥔 채 락을 기다려 2")
                assertNull(LockConnectionProbe.lastCallerTxOf("CreateOrderService.withOutCouponApproval"))
            } finally {
                TransactionSynchronizationManager.unbindResource(entityManagerFactory)
                em.close()
            }
        }

        @Test
        fun `락 선택 - 승인형(두둥·무료 승인형)은 티켓 락, 무료 선착순(결제형)은 예전 사용자 락`() {
            val shop = Shop()
            fun lockOf(ticketId: Long): String {
                val buyer = newBuyer()
                val cartId = v1Cart(buyer, ticketId, quantity = 1).andExpect { status { isOk() } }.data().at("/cartId").asLong()
                LockConnectionProbe.clearCalls()
                v1CreateOrder(buyer, cartId).andExpect { status { isOk() } }
                return listOf("CreateOrderService.withOutCouponApproval", "CreateOrderService.withOutCoupon").single { LockConnectionProbe.wasCalled(it) }
            }
            val dudoong = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "두둥락"))
            assertEquals("CreateOrderService.withOutCouponApproval", lockOf(dudoong), "두둥티켓 → 티켓 락")
            assertEquals("CreateOrderService.withOutCouponApproval", lockOf(freeTicket(shop, approvalRequired = true, name = "무료승인락")), "무료 승인형 → 티켓 락")
            assertEquals("CreateOrderService.withOutCoupon", lockOf(freeTicket(shop, approvalRequired = false, name = "무료선착순락")), "무료 선착순 → 사용자 락")
        }

        @Test
        fun `락 안에서 장바구니의 티켓이 락 키와 다르면 Order_400_2 (락 키를 잘못 넘긴 호출 방어)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val cartId = v1Cart(buyer, shop.ticketId, quantity = 1, answers = v1Answers(buyer, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }.data().at("/cartId").asLong()
            val other = createTicket(shop.team.manager, shop.eventId, dudoongBody(name = "다른티켓"))
            val e = assertThrows<DuDoongCodeException> { createOrderService.withOutCouponApproval(other, cartId, buyer.id!!) }
            assertEquals("Order_400_2", e.errorCode.getErrorReason().code)
            assertEquals(0L, pendingQuantity(shop.ticketId))
        }

        @Test
        fun `없는 장바구니·남의 장바구니는 예전과 같은 Cart_404_1`() {
            val owner = newBuyer()
            val shop = Shop()
            val cartId = v1Cart(owner, shop.ticketId, quantity = 1, answers = v1Answers(owner, shop.eventId, shop.ticketId)).andExpect { status { isOk() } }.data().at("/cartId").asLong()
            assertEquals("Cart_404_1", v1CreateOrder(newBuyer(), cartId).andExpect { status { isNotFound() } }.code())
            assertEquals("Cart_404_1", v1CreateOrder(owner, 999_999L).andExpect { status { isNotFound() } }.code())
        }
    }
}
