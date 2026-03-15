package band.gosrock.helper.excel

import band.gosrock.domain.common.vo.Money
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus
import java.time.LocalDateTime

data class ExcelOrderDto(
    val orderNo: String,
    val orderMethod: OrderMethod,
    val orderStatus: OrderStatus,
    val orderName: String,
    val userId: Long,
    val amount: Money,
    val quantity: Long,
    val createdAt: LocalDateTime,
    val refundAt: LocalDateTime?,
) {
    companion object {
        fun from(order: Order): ExcelOrderDto =
            ExcelOrderDto(
                amount = order.getTotalPaymentPrice(),
                orderNo = order.orderNo!!,
                orderMethod = order.orderMethod!!,
                orderStatus = order.orderStatus,
                orderName = order.orderName!!,
                userId = order.userId!!,
                quantity = order.getTotalQuantity(),
                createdAt = order.createdAt!!,
                refundAt = order.withDrawAt,
            )
    }
}
