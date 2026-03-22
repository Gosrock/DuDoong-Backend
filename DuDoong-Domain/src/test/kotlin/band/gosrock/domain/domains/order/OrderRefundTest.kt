package band.gosrock.domain.domains.order

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class OrderRefundTest {

    @Test
    fun `초기 refundStatus는 NONE이다`() {
        val order = createTestOrder()
        assertEquals(RefundStatus.NONE, order.refundStatus)
        assertNull(order.refundStatusChangedAt)
    }

    @Test
    fun `forTest로 cancel 상태와 REFUND_REQUESTED를 설정할 수 있다`() {
        val order = Order.forTest(
            userId = 1L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.CANCELED,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
            cancelReason = "단순 변심",
            refundStatus = RefundStatus.REFUND_REQUESTED,
        )

        assertEquals(OrderStatus.CANCELED, order.orderStatus)
        assertEquals("단순 변심", order.cancelReason)
        assertEquals(RefundStatus.REFUND_REQUESTED, order.refundStatus)
    }

    @Test
    fun `forTest로 refund 상태와 reason을 설정할 수 있다`() {
        val order = Order.forTest(
            userId = 1L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.REFUND,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
            cancelReason = "환불 요청합니다",
            refundStatus = RefundStatus.REFUND_REQUESTED,
        )

        assertEquals(OrderStatus.REFUND, order.orderStatus)
        assertEquals("환불 요청합니다", order.cancelReason)
        assertEquals(RefundStatus.REFUND_REQUESTED, order.refundStatus)
    }

    @Test
    fun `completeRefund 호출 시 refundStatus가 REFUND_COMPLETED로 변경된다`() {
        val order = Order.forTest(
            userId = 1L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.CANCELED,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
            cancelReason = "단순 변심",
            refundStatus = RefundStatus.REFUND_REQUESTED,
        )
        order.completeRefund()

        assertEquals(RefundStatus.REFUND_COMPLETED, order.refundStatus)
        assertNotNull(order.refundStatusChangedAt)
    }

    @Test
    fun `fail에 reason을 전달하면 failReason이 설정된다`() {
        val order = createTestOrder()
        order.fail("결제 실패")

        assertEquals(OrderStatus.FAILED, order.orderStatus)
        assertEquals("결제 실패", order.failReason)
    }

    @Test
    fun `fail에 reason 없이 호출하면 failReason이 null이다`() {
        val order = createTestOrder()
        order.fail()

        assertEquals(OrderStatus.FAILED, order.orderStatus)
        assertNull(order.failReason)
    }

    @Test
    fun `fail 시 501자 reason이 500자로 truncate된다`() {
        val order = createTestOrder()
        val longReason = "가".repeat(501)
        order.fail(longReason)

        assertEquals(500, order.failReason!!.length)
    }

    private fun createTestOrder(): Order = Order.forTest(
        userId = 1L,
        orderName = "테스트주문",
        orderStatus = OrderStatus.CONFIRM,
        orderMethod = OrderMethod.PAYMENT,
        eventId = 100L,
    )
}
