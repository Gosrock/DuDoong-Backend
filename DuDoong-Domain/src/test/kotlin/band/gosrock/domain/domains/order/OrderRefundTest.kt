package band.gosrock.domain.domains.order

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OrderRefundTest {

    private lateinit var order: Order

    @BeforeEach
    fun setUp() {
        order = Order.forTest(
            userId = 1L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.CONFIRM,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
        )
    }

    @Test
    fun `requestRefund 호출 시 userRefundReason이 설정되고 refundStatus가 REFUND_REQUESTED로 변경된다`() {
        order.requestRefund("단순 변심")

        assertEquals("단순 변심", order.userRefundReason)
        assertEquals(RefundStatus.REFUND_REQUESTED, order.refundStatus)
        assertNotNull(order.refundStatusChangedAt)
    }

    @Test
    fun `completeRefund 호출 시 refundStatus가 REFUND_COMPLETED로 변경된다`() {
        order.requestRefund("단순 변심")
        order.completeRefund()

        assertEquals(RefundStatus.REFUND_COMPLETED, order.refundStatus)
        assertNotNull(order.refundStatusChangedAt)
    }

    @Test
    fun `rejectRefund 호출 시 refundStatus가 REFUND_REJECTED로 변경되고 cancelReason이 설정된다`() {
        order.requestRefund("단순 변심")
        order.rejectRefund("환불 불가 기간")

        assertEquals(RefundStatus.REFUND_REJECTED, order.refundStatus)
        assertEquals("환불 불가 기간", order.cancelReason)
        assertNotNull(order.refundStatusChangedAt)
    }

    @Test
    fun `rejectRefund를 reason 없이 호출하면 cancelReason이 null이다`() {
        order.requestRefund("단순 변심")
        order.rejectRefund()

        assertEquals(RefundStatus.REFUND_REJECTED, order.refundStatus)
        assertNull(order.cancelReason)
    }

    @Test
    fun `fail에 reason을 전달하면 failReason이 설정된다`() {
        order.fail("결제 실패")

        assertEquals(OrderStatus.FAILED, order.orderStatus)
        assertEquals("결제 실패", order.failReason)
    }

    @Test
    fun `fail에 reason 없이 호출하면 failReason이 null이다`() {
        order.fail()

        assertEquals(OrderStatus.FAILED, order.orderStatus)
        assertNull(order.failReason)
    }

    @Test
    fun `초기 refundStatus는 NONE이다`() {
        assertEquals(RefundStatus.NONE, order.refundStatus)
        assertNull(order.refundStatusChangedAt)
    }
}
