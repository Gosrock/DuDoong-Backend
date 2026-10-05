package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
import band.gosrock.domain.domains.notification.domain.NotificationTargetType
import band.gosrock.domain.domains.notification.domain.NotificationType
import band.gosrock.domain.domains.notification.repository.NotificationRepository
import band.gosrock.domain.domains.notification.service.v2.V2NotificationDomainService
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.domain.User
import com.fasterxml.jackson.databind.JsonNode
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
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
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.put

/** 환불 계좌 입력·수정 O-5 (#728): 상태별 허용·거부(v2 주문만), 본인 주문만, O-3 표시(마스킹·입력 필요), 호스트 노출 범위, 변경 알림, 환불 완료와의 경합 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 사용자 앱 - 환불 계좌 입력")
class V2RefundAccountInputTest : V2UserOrderTestSupport() {

    @Autowired private lateinit var notificationRepository: NotificationRepository

    @Autowired private lateinit var notificationDomainService: V2NotificationDomainService

    private val account2 = mapOf("bankName" to " 우리은행 ", "accountHolder" to " 김철수 ", "accountNumber" to "1002 123 456789")

    private fun putAccount(user: User?, orderUuid: String, body: Map<String, Any?>): ResultActionsDsl =
        mockMvc.put("/api/v2/me/orders/$orderUuid/refund-account") {
            user?.let { with(auth(it)) }
            contentType = MediaType.APPLICATION_JSON
            content = json(body)
        }

    private fun detail(user: User, orderUuid: String): JsonNode = myOrder(user, orderUuid).andExpect { status { isOk() } }.data()

    private fun savedAccount(orderUuid: String) = refundAccountRepository.findByOrderId(orderRepository.findByOrderUuid(orderUuid).get().id!!)

    /** 승인 대기 유료 주문 → v2 거절 */
    private fun refused(shop: Shop, buyer: User, reasonType: String = "AMOUNT_MISMATCH"): String =
        v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            .also { refuse(shop.team.manager, shop.eventId, it, reasonType).andExpect { status { isOk() } } }

    /** v1 주문처럼 결제 채널을 지운다 (v1 앱 주문은 payment_channel 이 없다) */
    private fun asV1Order(orderUuid: String) {
        val order = orderRepository.findByOrderUuid(orderUuid).get()
        ReflectionTestUtils.setField(order, "paymentChannel", null)
        orderRepository.save(order)
    }

    private fun changedNotifications(user: User, orderUuid: String) =
        notificationRepository.findAllByUserId(user.id!!).filter { it.type == NotificationType.REFUND_ACCOUNT_CHANGED && it.targetId == orderUuid }

    private fun awaitChanged(user: User, orderUuid: String, count: Int) {
        val deadline = System.currentTimeMillis() + 10_000
        while (System.currentTimeMillis() < deadline && changedNotifications(user, orderUuid).size < count) Thread.sleep(50)
        assertEquals(count, changedNotifications(user, orderUuid).size, "$orderUuid 환불 계좌 변경 알림 수")
    }

    /** 승인된 유료 주문 → v2 호스트 취소 */
    private fun hostCanceled(shop: Shop, buyer: User): String =
        v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText().also {
            v1Approve(shop.team.master, shop.eventId, it).andExpect { status { isOk() } }
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$it/cancel", mapOf("reason" to "공연 취소")).andExpect { status { isOk() } }
        }

    private fun completeRefund(shop: Shop, orderUuid: String) =
        v2Post(shop.team.manager, "/events/${shop.eventId}/refunds/$orderUuid/complete").andExpect { status { isOk() } }

    @Nested
    @DisplayName("상태별 허용")
    inner class Allowed {

        @Test
        fun `호스트 거절 - O-3 입력 필요 표시, 입력하면 마스킹 노출·정규화 저장, 다시 입력하면 수정(1행)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = refused(shop, buyer)
            val before = detail(buyer, orderUuid)
            assertTrue(before.at("/refundAccountRequired").asBoolean())
            assertTrue(before.at("/refundAccountEditable").asBoolean())
            assertTrue(before.at("/refundAccount").isNull)

            val after = putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }.data()
            assertFalse(after.at("/refundAccountRequired").asBoolean())
            assertTrue(after.at("/refundAccountEditable").asBoolean())
            assertEquals("국민은행", after.at("/refundAccount/bankName").asText())
            assertEquals("*********8901", after.at("/refundAccount/maskedAccountNumber").asText())
            assertFalse(after.toString().contains("123-45-678901"), "주문자 응답에 계좌번호 전체가 없다")
            assertEquals("123-45-678901", savedAccount(orderUuid)!!.accountNumber)

            putAccount(buyer, orderUuid, account2).andExpect { status { isOk() } }
            val changed = savedAccount(orderUuid)!!
            assertEquals("우리은행", changed.bankName)
            assertEquals("김철수", changed.accountHolder)
            assertEquals("1002123456789", changed.accountNumber, "O-4 와 같이 공백을 지워 저장")
            assertEquals(1, refundAccountRepository.findByOrderIdIn(listOf(changed.orderId)).size)
        }

        @Test
        fun `호스트 취소(승인 후) - 입력 가능, 호스트 R-2·F-1 에는 매니저 이상만 전체 계좌, 일반 멤버는 null`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = hostCanceled(shop, buyer)
            assertTrue(detail(buyer, orderUuid).at("/refundAccountRequired").asBoolean())
            putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }

            val hostView = hostDetail(shop.team.manager, shop.eventId, orderUuid)
            assertEquals("123-45-678901", hostView.at("/refundAccount/accountNumber").asText())
            assertFalse(hostView.at("/refundAccount/updatedAt").isNull, "송금 전 확인용 마지막 입력·수정 시각")
            assertEquals("123-45-678901", hostDetail(shop.team.master, shop.eventId, orderUuid).at("/refundAccount/accountNumber").asText())
            assertTrue(hostDetail(shop.team.guest, shop.eventId, orderUuid).at("/refundAccount").isNull)
            fun refundRow(user: User) = v2Get(user, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data()
                .at("/content").first { it.at("/orderUuid").asText() == orderUuid }
            assertEquals("홍길동", refundRow(shop.team.manager).at("/refundAccount/accountHolder").asText())
            assertFalse(refundRow(shop.team.manager).at("/refundAccount/updatedAt").isNull)
            assertTrue(refundRow(shop.team.guest).at("/refundAccount").isNull)
        }

        @Test
        fun `사용자 취소(O-4 에서 계좌 입력) - 입력 필요 아님, 수정 가능`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }
            val d = detail(buyer, orderUuid)
            assertFalse(d.at("/refundAccountRequired").asBoolean())
            assertTrue(d.at("/refundAccountEditable").asBoolean())
            putAccount(buyer, orderUuid, account2).andExpect { status { isOk() } }
            assertEquals("우리은행", savedAccount(orderUuid)!!.bankName)
        }

        @Test
        fun `입금 미확인 거절 - 입력 필요 아님(안내 없음), 입력·수정은 가능`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = refused(shop, buyer, "DEPOSIT_UNCONFIRMED")
            val d = detail(buyer, orderUuid)
            assertFalse(d.at("/refundAccountRequired").asBoolean())
            assertTrue(d.at("/refundAccountEditable").asBoolean())
            putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }
            assertEquals("국민은행", savedAccount(orderUuid)!!.bankName)
        }
    }

    @Nested
    @DisplayName("변경 알림 (#728 리뷰)")
    inner class ChangeNotification {

        @Test
        fun `이미 있던 계좌를 바꾸면 호스트 마스터·매니저에게 REFUND_ACCOUNT_CHANGED, 첫 입력·같은 값 재입력은 알림 없음, 바꿀 때마다 1건`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = refused(shop, buyer)
            putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }
            putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }
            putAccount(buyer, orderUuid, account2).andExpect { status { isOk() } }
            awaitChanged(shop.team.master, orderUuid, 1)
            awaitChanged(shop.team.manager, orderUuid, 1)
            putAccount(buyer, orderUuid, refundAccount).andExpect { status { isOk() } }
            awaitChanged(shop.team.manager, orderUuid, 2)
            awaitChanged(shop.team.master, orderUuid, 2)
            val n = changedNotifications(shop.team.manager, orderUuid).first()
            assertEquals(NotificationTargetType.ORDER, n.targetType)
            assertTrue(n.body.contains("환불 계좌가 변경되었습니다"), n.body)
            assertTrue(changedNotifications(shop.team.guest, orderUuid).isEmpty(), "일반 멤버는 대상 아님")
            assertTrue(changedNotifications(buyer, orderUuid).isEmpty())

            // 변경 알림이 처리되기 전에 환불 완료됐으면(비동기 핸들러) 저장하지 않는다 — 이미 송금이 끝난 주문
            completeRefund(shop, orderUuid)
            assertEquals(0, notificationDomainService.notifyRefundAccountChanged(orderUuid, "after-complete"))
        }

        @Test
        fun `첫 입력만 하면 변경 알림 없음 (O-4 취소 때 입력 후 O-5 로 바꾸면 알림)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val first = refused(shop, buyer)
            putAccount(buyer, first, refundAccount).andExpect { status { isOk() } }
            val canceled = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            cancelMy(buyer, canceled, refundAccount).andExpect { status { isOk() } }
            putAccount(buyer, canceled, account2).andExpect { status { isOk() } }
            awaitChanged(shop.team.manager, canceled, 1)
            assertTrue(changedNotifications(shop.team.manager, first).isEmpty())
        }
    }

    @Nested
    @DisplayName("거부")
    inner class Rejected {

        @Test
        fun `환불 완료 뒤에는 Order_400_28 (v2 F-2·v1 완료 모두), 계좌 그대로, O-3 수정 불가`() {
            val shop = Shop()
            val buyer = newBuyer()
            val v2Done = refused(shop, buyer)
            putAccount(buyer, v2Done, refundAccount).andExpect { status { isOk() } }
            completeRefund(shop, v2Done)
            assertEquals("Order_400_28", putAccount(buyer, v2Done, account2).andExpect { status { isBadRequest() } }.code())
            assertEquals("국민은행", savedAccount(v2Done)!!.bankName)
            val d = detail(buyer, v2Done)
            assertFalse(d.at("/refundAccountEditable").asBoolean())
            assertFalse(d.at("/refundAccountRequired").asBoolean())

            // v1 환불 완료(주문 락 없음)도 같은 결과
            val v1Done = hostCanceled(shop, newBuyer())
            val owner = userRepository.findById(orderRepository.findByOrderUuid(v1Done).get().userId!!).get()
            mockMvc.patch("/api/v1/events/${shop.eventId}/refunds/$v1Done/complete") { with(auth(shop.team.master)) }.andExpect { status { isOk() } }
            assertEquals("Order_400_28", putAccount(owner, v1Done, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertNull(savedAccount(v1Done))
        }

        @Test
        fun `대상 아님 Order_400_27 - 승인 대기·승인 완료(환불 요청 없음), 무료 주문 취소`() {
            val shop = Shop()
            val buyer = newBuyer()
            val pending = v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            assertEquals("Order_400_27", putAccount(buyer, pending, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertFalse(detail(buyer, pending).at("/refundAccountEditable").asBoolean())
            val approved = shop.approved(newBuyer())
            val approvedOwner = userRepository.findById(orderRepository.findByOrderUuid(approved).get().userId!!).get()
            assertEquals("Order_400_27", putAccount(approvedOwner, approved, refundAccount).andExpect { status { isBadRequest() } }.code())

            val free = freeTicket(shop, approvalRequired = false)
            val freeBuyer = newBuyer()
            val freeOrder = v2OrderOk(freeBuyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$freeOrder/cancel").andExpect { status { isOk() } }
            assertEquals(RefundStatus.REFUND_REQUESTED, orderRepository.findByOrderUuid(freeOrder).get().refundStatus, "무료여도 v1 취소는 환불 요청 상태를 건다")
            assertEquals("Order_400_27", putAccount(freeBuyer, freeOrder, refundAccount).andExpect { status { isBadRequest() } }.code())
            assertFalse(detail(freeBuyer, freeOrder).at("/refundAccountRequired").asBoolean())

            // 무료 승인형(계좌이체와 같은 승인 방식이지만 0원) 거절 — 돌려줄 돈이 없어 대상 아님
            val freeApproval = freeTicket(shop, approvalRequired = true, name = "무료승인")
            val faBuyer = newBuyer()
            val faOrder = v2OrderOk(faBuyer, freeBodyOf(shop, freeApproval)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, faOrder, "SOLD_OUT").andExpect { status { isOk() } }
            assertEquals(RefundStatus.REFUND_REQUESTED, orderRepository.findByOrderUuid(faOrder).get().refundStatus)
            assertEquals("Order_400_27", putAccount(faBuyer, faOrder, refundAccount).andExpect { status { isBadRequest() } }.code())

            // 방어: 취소됐지만 환불 요청이 없는 유료 주문 (정상 흐름에는 없음)
            val noRequest = refused(shop, newBuyer())
            val noRequestOrder = orderRepository.findByOrderUuid(noRequest).get()
            ReflectionTestUtils.setField(noRequestOrder, "refundStatus", RefundStatus.NONE)
            orderRepository.save(noRequestOrder)
            val noRequestOwner = userRepository.findById(noRequestOrder.userId!!).get()
            assertEquals("Order_400_27", putAccount(noRequestOwner, noRequest, refundAccount).andExpect { status { isBadRequest() } }.code())
        }

        @Test
        fun `v1 주문(결제 채널 없음)은 환불 요청 중이어도·환불 완료여도 Order_400_27, O-3 입력 불가·필요 아님`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = refused(shop, buyer)
            asV1Order(orderUuid)
            assertEquals("Order_400_27", putAccount(buyer, orderUuid, refundAccount).andExpect { status { isBadRequest() } }.code())
            val d = detail(buyer, orderUuid)
            assertFalse(d.at("/refundAccountEditable").asBoolean())
            assertFalse(d.at("/refundAccountRequired").asBoolean())
            completeRefund(shop, orderUuid)
            assertEquals("Order_400_27", putAccount(buyer, orderUuid, refundAccount).andExpect { status { isBadRequest() } }.code(), "대상 판정이 완료 판정보다 먼저")
            assertNull(savedAccount(orderUuid))
        }

        @Test
        fun `남의 주문·없는 주문은 404, 비로그인 401, 계좌 검증 실패는 400 (O-4 와 같은 규칙)`() {
            val shop = Shop()
            val buyer = newBuyer()
            val orderUuid = refused(shop, buyer)
            assertEquals("Order_404_1", putAccount(newBuyer(), orderUuid, refundAccount).andExpect { status { isNotFound() } }.code())
            assertEquals("Order_404_1", putAccount(buyer, "no-such-order", refundAccount).andExpect { status { isNotFound() } }.code())
            putAccount(null, orderUuid, refundAccount).andExpect { status { isUnauthorized() } }
            putAccount(buyer, orderUuid, refundAccount + mapOf("bankName" to " ")).andExpect { status { isBadRequest() } }
            putAccount(buyer, orderUuid, refundAccount + mapOf("accountNumber" to "12ab34")).andExpect { status { isBadRequest() } }
            putAccount(buyer, orderUuid, refundAccount + mapOf("accountHolder" to "가".repeat(21))).andExpect { status { isBadRequest() } }
            assertNull(savedAccount(orderUuid))
        }
    }

    @Nested
    @DisplayName("알림 안내 문구 (#728 결정)")
    inner class NotificationGuide {

        private val guide = "주문상세에서 환불 계좌를 입력해 주세요."

        private fun body(user: User, type: NotificationType, orderUuid: String): String {
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline) {
                notificationRepository.findAllByUserId(user.id!!).firstOrNull { it.type == type && it.targetId == orderUuid }?.let { return it.body }
                Thread.sleep(50)
            }
            throw AssertionError("$type 알림 없음")
        }

        @Test
        fun `유료 계좌이체 거절·호스트 취소 알림에는 계좌 입력 안내, 대상은 주문상세`() {
            val shop = Shop()
            val refusedBuyer = newBuyer()
            val refusedOrder = refused(shop, refusedBuyer)
            assertTrue(body(refusedBuyer, NotificationType.ORDER_REFUSED, refusedOrder).endsWith(guide))
            val n = notificationRepository.findAllByUserId(refusedBuyer.id!!).first { it.type == NotificationType.ORDER_REFUSED }
            assertEquals(NotificationTargetType.ORDER, n.targetType)
            val canceledBuyer = newBuyer()
            val canceledOrder = hostCanceled(shop, canceledBuyer)
            assertTrue(body(canceledBuyer, NotificationType.ORDER_CANCELED_BY_HOST, canceledOrder).endsWith(guide))
        }

        @Test
        fun `무료 주문(즉시 발급 호스트 취소·승인형 거절)에는 안내 없음`() {
            val shop = Shop()
            val free = freeTicket(shop, approvalRequired = false)
            val freeBuyer = newBuyer()
            val freeOrder = v2OrderOk(freeBuyer, freeBodyOf(shop, free)).at("/orderUuid").asText()
            v2Post(shop.team.manager, "/events/${shop.eventId}/orders/$freeOrder/cancel").andExpect { status { isOk() } }
            assertFalse(body(freeBuyer, NotificationType.ORDER_CANCELED_BY_HOST, freeOrder).contains(guide))

            val freeApproval = freeTicket(shop, approvalRequired = true, name = "무료승인")
            val faBuyer = newBuyer()
            val faOrder = v2OrderOk(faBuyer, freeBodyOf(shop, freeApproval)).at("/orderUuid").asText()
            refuse(shop.team.manager, shop.eventId, faOrder, "SOLD_OUT").andExpect { status { isOk() } }
            assertFalse(body(faBuyer, NotificationType.ORDER_REFUSED, faOrder).contains(guide))
        }

        @Test
        fun `입금 미확인 거절·v1 주문 거절에는 안내 없음`() {
            val shop = Shop()
            val unconfirmedBuyer = newBuyer()
            val unconfirmed = refused(shop, unconfirmedBuyer, "DEPOSIT_UNCONFIRMED")
            assertFalse(body(unconfirmedBuyer, NotificationType.ORDER_REFUSED, unconfirmed).contains(guide))

            val v1Buyer = newBuyer()
            val v1Order = v2OrderOk(v1Buyer, shopBody(shop)).at("/orderUuid").asText()
            asV1Order(v1Order)
            refuse(shop.team.manager, shop.eventId, v1Order, "AMOUNT_MISMATCH").andExpect { status { isOk() } }
            assertFalse(body(v1Buyer, NotificationType.ORDER_REFUSED, v1Order).contains(guide))
        }
    }

    @Nested
    @DisplayName("동시성")
    inner class Concurrency {

        /**
         * H2 에서는 순서를 강제할 수 없어 어느 쪽이 먼저여도 맞는 결과인지만 본다 (잠금이 빠진 변형을 여기서는 못 잡는다).
         * 결정적 검증(완료가 먼저 대기 → 입력이 행 잠금 대기 → 해제 후 Order_400_28)은 MySQL E2E `test_51` test_05
         */
        @Test
        fun `계좌 입력 ↔ v1 환불 완료(주문 락 없음) 동시 - 완료가 먼저면 입력 거부(계좌 없음), 입력이 먼저면 계좌가 남고 완료 (결정적 검증은 E2E test_05)`() {
            repeat(3) {
                val shop = Shop()
                val buyer = newBuyer()
                val orderUuid = refused(shop, buyer)
                val start = CountDownLatch(1)
                val pool = Executors.newFixedThreadPool(2)
                val results = Collections.synchronizedMap(mutableMapOf<String, Int>())
                pool.submit { start.await(); results["put"] = putAccount(buyer, orderUuid, refundAccount).andReturn().response.status }
                // v1 환불 완료는 주문 락을 잡지 않는다 → 주문 행 잠금으로만 줄 선다
                pool.submit {
                    start.await()
                    results["complete"] = mockMvc.patch("/api/v1/events/${shop.eventId}/refunds/$orderUuid/complete") { with(auth(shop.team.master)) }.andReturn().response.status
                }
                start.countDown()
                pool.shutdown()
                assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS))
                assertEquals(200, results["complete"])
                assertEquals(RefundStatus.REFUND_COMPLETED, orderRepository.findByOrderUuid(orderUuid).get().refundStatus)
                if (results["put"] == 200) assertEquals("국민은행", savedAccount(orderUuid)!!.bankName) else assertNull(savedAccount(orderUuid))
            }
        }
    }
}
