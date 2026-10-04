package band.gosrock.domain.domains.order.service.v2

import band.gosrock.common.exception.DuDoongCodeException
import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.cart.domain.CartValidator
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.event.domain.Event
import band.gosrock.domain.domains.event.domain.EventStatus
import band.gosrock.domain.domains.issuedTicket.adaptor.IssuedTicketAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderItemVo
import band.gosrock.domain.domains.order.domain.OrderLineItem
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderPaymentChannel
import band.gosrock.domain.domains.order.domain.OrderRefundAccount
import band.gosrock.domain.domains.order.domain.OrderRefuseReasonType
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.order.repository.OrderRefundAccountRepository
import band.gosrock.domain.domains.order.service.OrderFactory
import band.gosrock.domain.domains.ticket_item.domain.TicketItem
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import band.gosrock.domain.domains.ticket_item.service.v2.V2TicketItemDomainService
import java.time.LocalDateTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.test.util.ReflectionTestUtils

/** v2 사용자 주문 규칙 (#718): 결제 방식·입금자명, 취소 가능 판정, 사용자 상태 분류, 사용자 철회 전이, 계좌 마스킹 */
class V2UserOrderDomainServiceTest {

    private val query = mock(V2UserOrderQuery::class.java)
    private val service = V2UserOrderDomainService(
        mock(OrderAdaptor::class.java), mock(OrderValidator::class.java), mock(OrderFactory::class.java), mock(CartValidator::class.java),
        mock(EventAdaptor::class.java), mock(OrderRefundAccountRepository::class.java), mock(V2TicketItemDomainService::class.java), query,
        mock(IssuedTicketAdaptor::class.java),
    )

    private val now = LocalDateTime.of(2026, 10, 4, 12, 0)

    private fun code(e: DuDoongCodeException) = e.errorCode.getErrorReason().code

    private fun item(payType: TicketPayType) = TicketItem(payType = payType, name = "티켓", price = Money.wons(1000), eventId = 1L)

    private fun event(status: EventStatus = EventStatus.OPEN, startAt: LocalDateTime = now.plusDays(1)) =
        Event(hostId = 1L, name = "공연", startAt = startAt, runTime = 120L).also { ReflectionTestUtils.setField(it, "status", status) }

    private fun order(status: OrderStatus, price: Long = 6000, method: OrderMethod = OrderMethod.APPROVAL, refund: RefundStatus = RefundStatus.NONE): Order {
        val vo = OrderItemVo().also {
            ReflectionTestUtils.setField(it, "price", Money.wons(price))
            ReflectionTestUtils.setField(it, "itemId", 10L)
            ReflectionTestUtils.setField(it, "itemGroupId", 1L)
            ReflectionTestUtils.setField(it, "name", "티켓")
        }
        return Order.forTest(
            userId = 7L, orderName = "티켓 1매", orderStatus = status, orderMethod = method, eventId = 1L, refundStatus = refund,
            orderLineItems = listOf(OrderLineItem.forTest(quantity = 1L, orderItemVo = vo)),
        ).also { ReflectionTestUtils.setField(it, "uuid", "uuid-1") }
    }

    @Nested
    inner class Payment {

        @Test
        fun `두둥티켓 - 계좌이체·토스만, 입금자명 앞뒤 공백 제거 1~20자`() {
            val dudoong = item(TicketPayType.DUDOONG_TICKET)
            assertEquals("홍길동", service.validatePayment(dudoong, OrderPaymentChannel.BANK_TRANSFER, "  홍길동 "))
            assertEquals("가".repeat(20), service.validatePayment(dudoong, OrderPaymentChannel.TOSS_TRANSFER, "가".repeat(20)))
            assertEquals("Order_400_21", code(assertThrows { service.validatePayment(dudoong, OrderPaymentChannel.FREE, "홍길동") }))
            listOf(null, "", "   ", "가".repeat(21)).forEach {
                assertEquals("Order_400_22", code(assertThrows { service.validatePayment(dudoong, OrderPaymentChannel.BANK_TRANSFER, it) }))
            }
        }

        @Test
        fun `무료 - FREE 만, 입금자명은 저장 안 함, PG 티켓은 Order_400_20`() {
            val free = item(TicketPayType.FREE_TICKET)
            assertNull(service.validatePayment(free, OrderPaymentChannel.FREE, "무시"))
            assertEquals("Order_400_21", code(assertThrows { service.validatePayment(free, OrderPaymentChannel.BANK_TRANSFER, null) }))
            assertEquals("Order_400_20", code(assertThrows { service.validatePayment(item(TicketPayType.PRICE_TICKET), OrderPaymentChannel.BANK_TRANSFER, "a") }))
        }
    }

    @Nested
    inner class CanCancel {

        @Test
        fun `승인 대기 - 공연 OPEN + 시작 전이면 가능 (입장 티켓 조회 안 함)`() {
            assertTrue(service.canCancel(order(OrderStatus.PENDING_APPROVE), event(), now))
            assertFalse(service.canCancel(order(OrderStatus.PENDING_APPROVE), event(startAt = now), now))
            assertFalse(service.canCancel(order(OrderStatus.PENDING_APPROVE), event(status = EventStatus.CALCULATING), now))
        }

        @Test
        fun `승인 완료 - 시작 전 + 입장·양도 티켓 없음`() {
            `when`(query.countCancelBlockingTickets(anyString(), anyLong())).thenReturn(0L)
            assertTrue(service.canCancel(order(OrderStatus.APPROVED), event(), now))
            assertTrue(service.canCancel(order(OrderStatus.APPROVED, price = 0, method = OrderMethod.PAYMENT), event(), now))
            `when`(query.countCancelBlockingTickets(anyString(), anyLong())).thenReturn(1L)
            assertFalse(service.canCancel(order(OrderStatus.APPROVED), event(), now))
        }

        @Test
        fun `거절·취소·환불·결제 진행 중·카드(PG) 결제 주문은 불가`() {
            `when`(query.countCancelBlockingTickets(anyString(), anyLong())).thenReturn(0L)
            listOf(OrderStatus.CANCELED, OrderStatus.REFUND, OrderStatus.PENDING_PAYMENT, OrderStatus.FAILED, OrderStatus.READY).forEach {
                assertFalse(service.canCancel(order(it), event(), now), "$it")
            }
            assertFalse(service.canCancel(order(OrderStatus.CONFIRM, price = 5000, method = OrderMethod.PAYMENT), event(), now))
        }
    }

    @Nested
    inner class MyStatus {

        private fun of(status: OrderStatus, refund: RefundStatus = RefundStatus.NONE, approvedAt: LocalDateTime? = null, reason: OrderRefuseReasonType? = null) =
            V2MyOrderStatus.of(
                order(status, refund = refund).also {
                    ReflectionTestUtils.setField(it, "approvedAt", approvedAt)
                    ReflectionTestUtils.setField(it, "refuseReasonType", reason)
                },
            )

        @Test
        fun `분류 - 환불 요청이 있는 REFUND 는 REFUNDED, 없는 REFUND(무료 취소)는 CANCELED, 거절·호스트 취소는 호스트 분류와 같음`() {
            assertEquals(V2MyOrderStatus.PENDING_APPROVE, of(OrderStatus.PENDING_APPROVE))
            assertEquals(V2MyOrderStatus.APPROVED, of(OrderStatus.APPROVED, approvedAt = now))
            assertEquals(V2MyOrderStatus.APPROVED, of(OrderStatus.CONFIRM, approvedAt = now))
            assertEquals(V2MyOrderStatus.REFUSED, of(OrderStatus.CANCELED, RefundStatus.REFUND_REQUESTED))
            assertEquals(V2MyOrderStatus.REFUSED, of(OrderStatus.CANCELED, approvedAt = now, reason = OrderRefuseReasonType.ETC))
            assertEquals(V2MyOrderStatus.CANCELED, of(OrderStatus.CANCELED, RefundStatus.REFUND_REQUESTED, approvedAt = now))
            assertEquals(V2MyOrderStatus.CANCELED, of(OrderStatus.REFUND))
            assertEquals(V2MyOrderStatus.REFUNDED, of(OrderStatus.REFUND, RefundStatus.REFUND_REQUESTED))
            assertEquals(V2MyOrderStatus.REFUNDED, of(OrderStatus.REFUND, RefundStatus.REFUND_COMPLETED))
        }

        @Test
        fun `목록 제외 - READY, PENDING_PAYMENT, FAILED, OUTDATED`() {
            listOf(OrderStatus.READY, OrderStatus.PENDING_PAYMENT, OrderStatus.FAILED, OrderStatus.OUTDATED).forEach { assertNull(of(it), "$it") }
        }
    }

    @Nested
    inner class Entity {

        @Test
        fun `withdrawByUser - REFUND, 환불 요청은 유료일 때만, 철회 시각 기록`() {
            val paid = order(OrderStatus.PENDING_APPROVE)
            paid.withdrawByUser(refundRequested = true)
            assertEquals(OrderStatus.REFUND, paid.orderStatus)
            assertEquals(RefundStatus.REFUND_REQUESTED, paid.refundStatus)
            assertTrue(paid.withDrawAt != null && paid.refundStatusChangedAt != null)

            val free = order(OrderStatus.APPROVED, price = 0)
            free.withdrawByUser(refundRequested = false)
            assertEquals(OrderStatus.REFUND, free.orderStatus)
            assertEquals(RefundStatus.NONE, free.refundStatus)
            assertNull(free.refundStatusChangedAt)
        }

        @Test
        fun `recordV2Payment - 결제 방식·입금자명 기록`() {
            val o = order(OrderStatus.PENDING_APPROVE)
            o.recordV2Payment(OrderPaymentChannel.TOSS_TRANSFER, "입금자")
            assertEquals(OrderPaymentChannel.TOSS_TRANSFER, o.paymentChannel)
            assertEquals("입금자", o.depositorName)
        }

        @Test
        fun `환불 계좌 마스킹 - 뒤 4자리만, 4자리 이하는 전부`() {
            fun mask(number: String) = OrderRefundAccount(orderId = 1L, bankName = "b", accountHolder = "h", accountNumber = number).maskedAccountNumber()
            assertEquals("*********8901", mask("123-45-678901"))
            assertEquals("****", mask("1234"))
            assertEquals("*2345", mask("12345"))
        }
    }
}
