package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminRefundStatusRequest
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.domain.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminUpdateRefundStatusUseCase")
class AdminUpdateRefundStatusUseCaseTest {

    @Mock
    private lateinit var orderAdaptor: OrderAdaptor

    @Mock
    private lateinit var userAdaptor: UserAdaptor

    @Mock
    private lateinit var eventAdaptor: EventAdaptor

    @Mock
    private lateinit var adminAuthValidator: AdminAuthValidator

    private lateinit var useCase: AdminUpdateRefundStatusUseCase

    private lateinit var order: Order

    @BeforeEach
    fun setUp() {
        useCase = AdminUpdateRefundStatusUseCase(orderAdaptor, userAdaptor, eventAdaptor, adminAuthValidator)
        order = Order.forTest(
            userId = 10L,
            orderName = "테스트주문",
            orderStatus = OrderStatus.CANCELED,
            orderMethod = OrderMethod.PAYMENT,
            eventId = 100L,
            cancelReason = "단순 변심",
            refundStatus = RefundStatus.REFUND_REQUESTED,
        )
    }

    private fun createAdminUser(): User {
        val user = User()
        ReflectionTestUtils.setField(user, "id", 1L)
        ReflectionTestUtils.setField(user, "accountRole", AccountRole.ADMIN)
        return user
    }

    @Test
    @DisplayName("환불 완료 처리 시 refundStatus가 REFUND_COMPLETED로 변경된다")
    fun completeRefund() {
        val adminUser = createAdminUser()
        `when`(adminAuthValidator.validateAdminOrAbove(1L)).thenReturn(adminUser)
        `when`(orderAdaptor.findByOrderUuid("test-uuid")).thenReturn(order)

        ReflectionTestUtils.setField(order, "uuid", "test-uuid")
        val request = AdminRefundStatusRequest(refundStatus = "REFUND_COMPLETED")

        useCase.execute(1L, "test-uuid", request)

        assertEquals(RefundStatus.REFUND_COMPLETED, order.refundStatus)
        assertNotNull(order.refundStatusChangedAt)
    }

    @Test
    @DisplayName("허용되지 않는 refundStatus 값이면 예외가 발생한다")
    fun invalidRefundStatus() {
        val adminUser = createAdminUser()
        `when`(adminAuthValidator.validateAdminOrAbove(1L)).thenReturn(adminUser)
        `when`(orderAdaptor.findByOrderUuid("test-uuid")).thenReturn(order)

        ReflectionTestUtils.setField(order, "uuid", "test-uuid")
        val request = AdminRefundStatusRequest(refundStatus = "NONE")

        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(1L, "test-uuid", request)
        }
    }
}
