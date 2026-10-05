package band.gosrock.api.v2.order

import band.gosrock.api.supports.ApiIntegrateSpringBootTest
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.put

/** 환불 계좌 입력·수정 O-5 (#728): 상태별 허용·거부, 본인 주문만, O-3 표시(마스킹·입력 필요), 호스트 노출 범위, 환불 완료와의 경합 */
@ApiIntegrateSpringBootTest
@AutoConfigureMockMvc
@DisplayName("v2 사용자 앱 - 환불 계좌 입력")
class V2RefundAccountInputTest : V2UserOrderTestSupport() {

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
    private fun refused(shop: Shop, buyer: User): String =
        v2OrderOk(buyer, shopBody(shop)).at("/orderUuid").asText()
            .also { refuse(shop.team.manager, shop.eventId, it, "DEPOSIT_UNCONFIRMED").andExpect { status { isOk() } } }

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

            assertEquals("123-45-678901", hostDetail(shop.team.manager, shop.eventId, orderUuid).at("/refundAccount/accountNumber").asText())
            assertEquals("123-45-678901", hostDetail(shop.team.master, shop.eventId, orderUuid).at("/refundAccount/accountNumber").asText())
            assertTrue(hostDetail(shop.team.guest, shop.eventId, orderUuid).at("/refundAccount").isNull)
            fun refundRow(user: User) = v2Get(user, "/events/${shop.eventId}/refunds").andExpect { status { isOk() } }.data()
                .at("/content").first { it.at("/orderUuid").asText() == orderUuid }
            assertEquals("홍길동", refundRow(shop.team.manager).at("/refundAccount/accountHolder").asText())
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
    @DisplayName("동시성")
    inner class Concurrency {

        @Test
        fun `계좌 입력 ↔ 환불 완료 동시 - 완료가 먼저면 입력 거부(계좌 없음), 입력이 먼저면 계좌가 남고 완료`() {
            repeat(3) {
                val shop = Shop()
                val buyer = newBuyer()
                val orderUuid = refused(shop, buyer)
                val start = CountDownLatch(1)
                val pool = Executors.newFixedThreadPool(2)
                val results = Collections.synchronizedMap(mutableMapOf<String, Int>())
                pool.submit { start.await(); results["put"] = putAccount(buyer, orderUuid, refundAccount).andReturn().response.status }
                pool.submit { start.await(); results["complete"] = v2Post(shop.team.manager, "/events/${shop.eventId}/refunds/$orderUuid/complete").andReturn().response.status }
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
