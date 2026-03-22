package band.gosrock.domain.domains.order.service.handler

import band.gosrock.domain.common.events.order.DoneOrderEvent
import band.gosrock.domain.domains.coupon.service.RecoveryCouponService
import band.gosrock.domain.domains.issuedTicket.service.IssuedTicketDomainService
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.service.WithdrawPaymentService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.nullable
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.reflect.Constructor

@ExtendWith(MockitoExtension::class)
class ConfirmOrderFailHandlerTest {

    @Mock
    lateinit var cancelPaymentService: WithdrawPaymentService

    @Mock
    lateinit var issuedTicketDomainService: IssuedTicketDomainService

    @Mock
    lateinit var recoveryCouponService: RecoveryCouponService

    @Mock
    lateinit var orderAdaptor: OrderAdaptor

    @InjectMocks
    lateinit var handler: ConfirmOrderFailHandler

    @Mock
    lateinit var order: Order

    companion object {
        private fun createDoneOrderEvent(userId: Long = 1L, paymentKey: String? = null): DoneOrderEvent {
            val constructor: Constructor<DoneOrderEvent> =
                DoneOrderEvent::class.java.getDeclaredConstructor(
                    String::class.java, Long::class.java,
                    OrderMethod::class.java,
                    String::class.java, Long::class.java
                )
            constructor.isAccessible = true
            return constructor.newInstance("test-order-uuid", userId, OrderMethod.APPROVAL, paymentKey, 100L)
        }
    }

    @Test
    fun `주문 실패 이벤트 시 주문을 실패 상태로 변경한다`() {
        // given
        val event = createDoneOrderEvent()
        given(orderAdaptor.findByOrderUuid("test-order-uuid")).willReturn(order)
        given(order.hasCoupon()).willReturn(false)
        given(order.isNeedPaid()).willReturn(false)

        // when
        handler.handleDoneOrderFailEvent(event)

        // then
        then(order).should(times(1)).fail(nullable(String::class.java))
    }

    @Test
    fun `주문 실패 이벤트 시 발급 티켓 회수를 실행한다`() {
        // given
        val event = createDoneOrderEvent()
        given(orderAdaptor.findByOrderUuid("test-order-uuid")).willReturn(order)
        given(order.hasCoupon()).willReturn(false)
        given(order.isNeedPaid()).willReturn(false)

        // when
        handler.handleDoneOrderFailEvent(event)

        // then
        then(issuedTicketDomainService).should(times(1))
            .doneOrderEventAfterRollBackWithdrawIssuedTickets(100L, "test-order-uuid")
    }

    @Test
    fun `쿠폰이 없는 주문 실패 시 쿠폰 복구를 실행하지 않는다`() {
        // given
        val event = createDoneOrderEvent()
        given(orderAdaptor.findByOrderUuid("test-order-uuid")).willReturn(order)
        given(order.hasCoupon()).willReturn(false)
        given(order.isNeedPaid()).willReturn(false)

        // when
        handler.handleDoneOrderFailEvent(event)

        // then
        then(recoveryCouponService).should(never()).execute(anyLong(), anyLong())
    }
}
