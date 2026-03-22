package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminRefundStatusRequest
import band.gosrock.admin.model.dto.response.AdminOrderResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.event.adaptor.EventAdaptor
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor
import band.gosrock.domain.domains.order.domain.RefundStatus
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateRefundStatusUseCase(
    private val orderAdaptor: OrderAdaptor,
    private val userAdaptor: UserAdaptor,
    private val eventAdaptor: EventAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, orderUuid: String, request: AdminRefundStatusRequest): AdminOrderResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val order = orderAdaptor.findByOrderUuid(orderUuid)

        when (RefundStatus.valueOf(request.refundStatus)) {
            RefundStatus.REFUND_COMPLETED -> order.completeRefund()
            else -> throw IllegalArgumentException("허용되지 않는 환불 상태입니다: ${request.refundStatus}")
        }

        val userName = order.userId?.let {
            runCatching { userAdaptor.queryUser(it).profile?.name }.getOrNull()
        }
        val eventName = order.eventId?.let {
            runCatching { eventAdaptor.findById(it).eventBasic?.name }.getOrNull()
        }
        return AdminOrderResponse.of(order, userName, eventName)
    }
}
