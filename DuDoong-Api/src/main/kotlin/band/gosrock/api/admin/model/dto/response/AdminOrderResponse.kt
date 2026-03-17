package band.gosrock.api.admin.model.dto.response

import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime

data class AdminOrderResponse(
    val orderId: String?,
    val userName: String?,
    val eventName: String?,
    val ticketName: String?,
    val totalAmount: String?,
    val orderStatus: OrderStatus,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun of(order: Order, userName: String?, eventName: String?): AdminOrderResponse =
            AdminOrderResponse(
                orderId = order.uuid,
                userName = userName,
                eventName = eventName,
                ticketName = order.orderName,
                totalAmount = order.getTotalPaymentPrice().toString(),
                orderStatus = order.orderStatus,
                createdAt = order.createdAt,
            )
    }
}
