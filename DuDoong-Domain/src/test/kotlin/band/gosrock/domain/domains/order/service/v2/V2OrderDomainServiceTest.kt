package band.gosrock.domain.domains.order.service.v2

import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.exception.InvalidRefuseReasonException
import band.gosrock.domain.domains.order.exception.OrderNotFoundException
import band.gosrock.domain.domains.order.exception.OrderRefundNotRequestedException
import band.gosrock.domain.domains.order.service.OrderApproveService
import band.gosrock.domain.domains.order.service.WithdrawOrderService
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** v2 주문 규칙 (#712): 상태 분류, 거절 사유 검증, 공연 소속 확인, 환불 완료 */
class V2OrderDomainServiceTest {

    private val orderAdaptor = mock(OrderAdaptor::class.java)
    private val orderApproveService = mock(OrderApproveService::class.java)
    private val withdrawOrderService = mock(WithdrawOrderService::class.java)
    private val v2OrderQuery = mock(V2OrderQuery::class.java)
    private val service = V2OrderDomainService(orderAdaptor, mock(OrderValidator::class.java), orderApproveService, withdrawOrderService, v2OrderQuery)

    private fun order(
        status: OrderStatus,
        approvedAt: LocalDateTime? = null,
        reasonType: OrderRefuseReasonType? = null,
        eventId: Long = 1L,
        refundStatus: RefundStatus = RefundStatus.NONE,
    ): Order = Order.forTest(orderStatus = status, eventId = eventId, refundStatus = refundStatus).also {
        ReflectionTestUtils.setField(it, "approvedAt", approvedAt)
        ReflectionTestUtils.setField(it, "refuseReasonType", reasonType)
    }

    @Nested
    inner class StatusMapping {
        private val now = LocalDateTime.now()

        @Test
        fun `승인 대기 - 승인 완료(APPROVED, CONFIRM)`() {
            assertEquals(V2OrderStatus.PENDING_APPROVE, V2OrderStatus.of(order(OrderStatus.PENDING_APPROVE)))
            assertEquals(V2OrderStatus.APPROVED, V2OrderStatus.of(order(OrderStatus.APPROVED, now)))
            assertEquals(V2OrderStatus.APPROVED, V2OrderStatus.of(order(OrderStatus.CONFIRM, now)))
        }

        @Test
        fun `CANCELED - v2 거절(사유 종류) 과 v1 거절(approvedAt 없음) 은 REFUSED, 승인 후 취소는 CANCELED`() {
            assertEquals(V2OrderStatus.REFUSED, V2OrderStatus.of(order(OrderStatus.CANCELED, reasonType = OrderRefuseReasonType.SOLD_OUT)))
            assertEquals(V2OrderStatus.REFUSED, V2OrderStatus.of(order(OrderStatus.CANCELED)))
            assertEquals(V2OrderStatus.CANCELED, V2OrderStatus.of(order(OrderStatus.CANCELED, now)))
        }

        @Test
        fun `사용자 환불(REFUND) 은 CANCELED, FAILED·OUTDATED 는 FAILED, 결제 진행 중은 목록 제외(null)`() {
            assertEquals(V2OrderStatus.CANCELED, V2OrderStatus.of(order(OrderStatus.REFUND, now)))
            assertEquals(V2OrderStatus.FAILED, V2OrderStatus.of(order(OrderStatus.FAILED)))
            assertEquals(V2OrderStatus.FAILED, V2OrderStatus.of(order(OrderStatus.OUTDATED)))
            assertNull(V2OrderStatus.of(order(OrderStatus.READY)))
            assertNull(V2OrderStatus.of(order(OrderStatus.PENDING_PAYMENT)))
        }
    }

    @Nested
    inner class RefuseReason {
        @ParameterizedTest
        @EnumSource(value = OrderRefuseReasonType::class, names = ["DEPOSIT_UNCONFIRMED", "AMOUNT_MISMATCH", "SOLD_OUT"])
        fun `고정 사유는 종류 문구 (입력값 무시)`(type: OrderRefuseReasonType) {
            assertEquals(type.label, service.refuseReasonText(type, "무시됨".repeat(10)))
            assertEquals(type.label, service.refuseReasonText(type, null))
        }

        @Test
        fun `기타는 앞뒤 공백 제외 1~20자`() {
            assertEquals("중복 주문", service.refuseReasonText(OrderRefuseReasonType.ETC, "  중복 주문 "))
            assertEquals("가".repeat(20), service.refuseReasonText(OrderRefuseReasonType.ETC, "가".repeat(20)))
            assertThrows<InvalidRefuseReasonException> { service.refuseReasonText(OrderRefuseReasonType.ETC, null) }
            assertThrows<InvalidRefuseReasonException> { service.refuseReasonText(OrderRefuseReasonType.ETC, "   ") }
            assertThrows<InvalidRefuseReasonException> { service.refuseReasonText(OrderRefuseReasonType.ETC, "가".repeat(21)) }
        }
    }

    @Nested
    inner class EventScope {
        @Test
        fun `다른 공연 주문은 404, 승인·취소는 v1 도메인 서비스로 넘기지 않는다`() {
            `when`(orderAdaptor.findByOrderUuid("u")).thenReturn(order(OrderStatus.PENDING_APPROVE, eventId = 2L))
            `when`(v2OrderQuery.findEventId("u")).thenReturn(2L)
            assertThrows<OrderNotFoundException> { service.queryEventOrder(1L, "u") }
            assertThrows<OrderNotFoundException> { service.approve(1L, "u") }
            assertThrows<OrderNotFoundException> { service.cancel(1L, "u", null) }
            verify(orderApproveService, never()).execute("u")
            verify(withdrawOrderService, never()).cancelOrder("u", null)
            // 없는 주문도 404
            assertThrows<OrderNotFoundException> { service.approve(1L, "none") }
        }

        @Test
        fun `같은 공연이면 v1 승인 서비스 그대로 호출, 취소 사유는 trim`() {
            val o = order(OrderStatus.PENDING_APPROVE)
            `when`(orderAdaptor.findByOrderUuid("u")).thenReturn(o)
            `when`(v2OrderQuery.findEventId("u")).thenReturn(1L)
            assertSame(o, service.queryEventOrder(1L, "u"))
            service.approve(1L, "u")
            verify(orderApproveService).execute("u")
            service.cancel(1L, "u", "  일정 변경 ")
            verify(withdrawOrderService).cancelOrder("u", "일정 변경")
        }
    }

    @Nested
    inner class CompleteRefund {
        @Test
        fun `환불 요청 → 완료, 이미 완료면 그대로, 요청 없음은 400`() {
            val requested = order(OrderStatus.CANCELED, refundStatus = RefundStatus.REFUND_REQUESTED)
            `when`(orderAdaptor.findByOrderUuid("r")).thenReturn(requested)
            service.completeRefund(1L, "r")
            assertEquals(RefundStatus.REFUND_COMPLETED, requested.refundStatus)
            val changedAt = requested.refundStatusChangedAt
            service.completeRefund(1L, "r")
            assertEquals(changedAt, requested.refundStatusChangedAt)

            `when`(orderAdaptor.findByOrderUuid("n")).thenReturn(order(OrderStatus.APPROVED, LocalDateTime.now()))
            assertThrows<OrderRefundNotRequestedException> { service.completeRefund(1L, "n") }
        }
    }
}
