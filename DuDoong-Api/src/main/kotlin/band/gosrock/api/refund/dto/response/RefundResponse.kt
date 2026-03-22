package band.gosrock.api.refund.dto.response

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.RefundStatus
import java.time.LocalDateTime

data class RefundResponse(
    val orderId: String?,
    val orderNo: String?,
    val userName: String?,
    val eventName: String?,
    val eventId: Long?,
    val ticketName: String?,
    val totalAmount: Long,
    val cancelReason: String?,
    val refundStatus: RefundStatus,
    val refundStatusChangedAt: LocalDateTime?,
    val withDrawAt: LocalDateTime?,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(order: Order, userName: String?, eventName: String?): RefundResponse =
            RefundResponse(
                orderId = order.uuid,
                orderNo = order.orderNo,
                userName = userName,
                eventName = eventName,
                eventId = order.eventId,
                ticketName = order.orderName,
                totalAmount = order.getTotalPaymentPrice().longValue(),
                cancelReason = order.cancelReason,
                refundStatus = order.refundStatus,
                refundStatusChangedAt = order.refundStatusChangedAt,
                withDrawAt = order.withDrawAt,
                createdAt = order.createdAt,
            )
    }
}
