package band.gosrock.admin.service

import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminCompleteRefundUseCase")
class AdminCompleteRefundUseCaseTest {

    @Mock
    private lateinit var orderAdaptor: OrderAdaptor

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    @Mock
    private lateinit var eventAdaptor: EventAdaptor

    @Mock
    private lateinit var adminAuthValidator: AdminAuthValidator

    @InjectMocks
    private lateinit var adminCompleteRefundUseCase: AdminCompleteRefundUseCase

    @Test
    @DisplayName("어드민 환불 확인 시 refundStatus가 REFUND_COMPLETED로 변경된다")
    fun completeRefund() {
        // given
        val order = Order.forTest(
            userId = 1L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.CANCELED,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
            cancelReason = "단순 변심",
            refundStatus = RefundStatus.REFUND_REQUESTED,
        )
        given(orderAdaptor.findByOrderUuid("test-uuid")).willReturn(order)

        // when
        val response = adminCompleteRefundUseCase.execute(1L, "test-uuid")

        // then
        assertEquals(RefundStatus.REFUND_COMPLETED, response.refundStatus)
        assertEquals(RefundStatus.REFUND_COMPLETED, order.refundStatus)
        assertNotNull(order.refundStatusChangedAt)
        assertNotNull(response.userId)
        assertEquals(1L, response.userId)
    }
}
