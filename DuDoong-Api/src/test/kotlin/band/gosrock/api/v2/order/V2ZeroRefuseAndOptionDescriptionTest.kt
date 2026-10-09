package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.api.v2.operation.usecase.V2OperationMapper
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.ticket_item.adaptor.OptionAdaptor
import band.gosrock.domain.domains.ticket_item.repository.OptionGroupRepository
import band.gosrock.domain.domains.user.domain.User
import jakarta.persistence.EntityManagerFactory
import org.hibernate.Hibernate
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
import org.springframework.test.web.servlet.post
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

/** 전수검사 3차 후속 (#752, 사용자 결정 2026-10-09): 0원 주문 v2 거절은 환불 요청 없음(v1 거절은 그대로), 옵션 답변 응답에 옵션 설명 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 0원 거절·옵션 설명")
class V2ZeroRefuseAndOptionDescriptionTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired private lateinit var transactionManager: PlatformTransactionManager

    @Autowired private lateinit var optionAdaptor: OptionAdaptor

    @Autowired private lateinit var optionGroupRepository: OptionGroupRepository

    @Autowired private lateinit var mapper: V2OperationMapper

    private fun v1Refuse(host: User, eventId: Long, orderUuid: String) =
        mockMvc.post("/api/v1/events/$eventId/orders/$orderUuid/refuse") {
            with(auth(host))
            contentType = MediaType.APPLICATION_JSON
            content = json(mapOf("reason" to "v1 거절"))
        }

    private fun refusedBody(user: User, orderUuid: String): String {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline) {
            notificationRepository.findAllByUserId(user.id!!).firstOrNull { it.type == NotificationType.ORDER_REFUSED && it.targetId == orderUuid }?.let { return it.body }
            Thread.sleep(50)
        }
        throw AssertionError("ORDER_REFUSED 알림 없음")
    }

    @Nested
    @DisplayName("0원 주문 거절 (A)")
    inner class ZeroRefuse {

        @Test
        fun `v2 거절 - 0원 주문은 환불 요청 없음, F-1·D-1 환불 요청에 없고 F-2 완료는 Order_400_17, 거절 상태·알림은 그대로`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = true)
            val buyer = newBuyer()
            val orderUuid = v2OrderOk(buyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, orderUuid, "SOLD_OUT").andExpect { status { isOk() } }

            val saved = orderRepository.findByOrderUuid(orderUuid).get()
            assertEquals(OrderStatus.CANCELED, saved.orderStatus)
            assertEquals(RefundStatus.NONE, saved.refundStatus)
            val host = hostDetail(shop.team.guest, shop.eventId, orderUuid)
            assertEquals("REFUSED", host.at("/order/status").asText())
            assertEquals("NONE", host.at("/order/refundStatus").asText())
            assertEquals(0, v2Get(shop.team.guest, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data().at("/totalElements").asLong())
            val dashboard = v2Get(shop.team.guest, "/events/${shop.eventId}/dashboard").andExpect { status { isOk() } }.data()
            assertEquals(0, dashboard.at("/orders/refundRequested").asLong())
            assertEquals(1, dashboard.at("/orders/refused").asLong())
            assertEquals("Order_400_17", v2Post(shop.team.manager, "/events/${shop.eventId}/refunds/$orderUuid/complete").andExpect { status { isBadRequest() } }.code())

            // O-3: 거절 표시는 그대로, 환불·계좌 입력 표시 없음. 거절 알림은 가고 계좌 입력 안내는 없다
            val mine = myOrder(buyer, orderUuid).andExpect { status { isOk() } }.data()
            assertEquals("REFUSED", mine.at("/status").asText())
            assertEquals("NONE", mine.at("/refundStatus").asText())
            assertFalse(mine.at("/refundAccountEditable").asBoolean())
            assertFalse(mine.at("/refundAccountRequired").asBoolean())
            assertFalse(refusedBody(buyer, orderUuid).contains("환불 계좌"))
        }

        @Test
        fun `v2 거절 - 유료 주문은 지금처럼 환불 요청`() {
            val shop = Shop()
            val orderUuid = v2OrderOk(newBuyer(), shopBody(shop)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, orderUuid, "AMOUNT_MISMATCH").andExpect { status { isOk() } }
            assertEquals(RefundStatus.REFUND_REQUESTED, orderRepository.findByOrderUuid(orderUuid).get().refundStatus)
            assertEquals(1, v2Get(shop.team.guest, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data().at("/totalElements").asLong())
        }

        @Test
        fun `v1 거절은 그대로 - 0원 주문도 환불 요청 (v1 동작 고정)`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = true)
            val orderUuid = v2OrderOk(newBuyer(), freeBodyOf(shop, free)).at("/orderUuid").asText()
            v1Refuse(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
            val saved = orderRepository.findByOrderUuid(orderUuid).get()
            assertEquals(OrderStatus.CANCELED, saved.orderStatus)
            assertEquals(RefundStatus.REFUND_REQUESTED, saved.refundStatus)
            assertEquals(1, v2Get(shop.team.guest, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data().at("/totalElements").asLong())
        }
    }

    @Nested
    @DisplayName("옵션 답변의 옵션 설명 (B)")
    inner class OptionDescription {

        @Test
        fun `O-3 옵션 답변(라인·발급 티켓)에 옵션 설명, 설명을 바꾸면 현재 설명`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            fun descriptions() = myOrder(buyer, orderUuid).andExpect { status { isOk() } }.data().let { d ->
                (d.at("/lines").flatMap { it.at("/optionAnswers") } + d.at("/issuedTickets").flatMap { it.at("/optionAnswers") })
                    .associate { it.at("/optionName").asText() to it.at("/description").asText() }
            }
            assertEquals(mapOf("뒷풀이" to "참석하나요?", "입금자명" to "참석하나요?"), descriptions())
            v1Approve(shop.team.master, shop.eventId, orderUuid).andExpect { status { isOk() } }
            patchOption(shop.team.manager, shop.eventId, shop.subjectiveOptionId, mapOf("description" to "입금하신 분 성함")).andExpect { status { isOk() } }
            assertEquals(mapOf("뒷풀이" to "참석하나요?", "입금자명" to "입금하신 분 성함"), descriptions())
            // 호스트 주문 상세(R-2)도 같은 응답 모양
            val host = hostDetail(shop.team.guest, shop.eventId, orderUuid)
            assertEquals("입금하신 분 성함", host.at("/issuedTickets/0/optionAnswers").first { it.at("/optionName").asText() == "입금자명" }.at("/description").asText())
        }

        @Test
        fun `옵션 질문은 옵션 그룹과 함께 쿼리 1개로 읽는다 (옵션 수와 무관, N+1 없음)`() {
            val shops = (1..3).map { Shop() }
            val tx = TransactionTemplate(transactionManager)
            val optionIds = tx.execute {
                shops.flatMap { shop -> optionGroupRepository.findAllById(listOf(shop.yesNoOptionId, shop.subjectiveOptionId)).flatMap { g -> g.options.map { it.id!! } } }
            }!!
            assertEquals(9, optionIds.size, "공연 3개 x (네·아니오 2행 + 주관식 1행)")
            val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
            fun queries(ids: List<Long>): Long = tx.execute {
                statistics.isStatisticsEnabled = true
                try {
                    statistics.clear()
                    val questions = mapper.optionQuestionsOf(ids)
                    assertEquals(ids.size, questions.size)
                    assertTrue(questions.values.all { it.name != null && it.description == "참석하나요?" })
                    statistics.prepareStatementCount
                } finally {
                    statistics.isStatisticsEnabled = false
                }
            }!!
            // 다른 테스트가 같은 JVM 에서 쿼리를 내면 전역 통계가 섞이므로 3번 중 최솟값
            assertEquals(1L, (1..3).minOf { queries(optionIds) }, "옵션 ${optionIds.size}개")
            tx.execute {
                assertTrue(optionAdaptor.findAllWithGroupByIds(optionIds).all { Hibernate.isInitialized(it.optionGroup) }, "옵션 그룹 fetch join")
            }
        }
    }
}
