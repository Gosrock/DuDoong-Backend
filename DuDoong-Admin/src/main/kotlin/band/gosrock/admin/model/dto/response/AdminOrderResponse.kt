package band.gosrock.admin.model.dto.response

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime

data class AdminOrderResponse(
    val orderId: String?,
    val userName: String?,
    val eventName: String?,
    val ticketName: String?,
    val totalAmount: Long,
    val orderStatus: OrderStatus,
    val createdAt: LocalDateTime?,
    val orderNo: String?,
    val orderMethod: String?,
    val userId: Long?,
    val eventId: Long?,
    val approvedAt: LocalDateTime?,
    val withDrawAt: LocalDateTime?,
    val paymentMethod: String?,
    val receiptUrl: String?,
    val supplyAmount: String?,
    val discountAmount: String?,
    val couponName: String?,
) {
    companion object {
        fun of(order: Order, userName: String?, eventName: String?): AdminOrderResponse =
            AdminOrderResponse(
                orderId = order.uuid,
                userName = userName,
                eventName = eventName,
                ticketName = order.orderName,
                totalAmount = order.getTotalPaymentPrice().longValue(),
                orderStatus = order.orderStatus,
                createdAt = order.createdAt,
                orderNo = order.orderNo,
                orderMethod = order.orderMethod?.name,
                userId = order.userId,
                eventId = order.eventId,
                approvedAt = order.approvedAt,
                withDrawAt = order.withDrawAt,
                paymentMethod = order.pgPaymentInfo.paymentMethod.name,
                receiptUrl = order.pgPaymentInfo.receiptUrl,
                supplyAmount = order.totalPaymentInfo?.supplyAmount?.toString(),
                discountAmount = order.totalPaymentInfo?.discountAmount?.toString(),
                couponName = order.orderCouponVo.name,
            )
    }
}
