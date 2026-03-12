package band.gosrock.domain.common.events.order

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod

class DoneOrderEvent private constructor(
    val orderUuid: String,
    val userId: Long,
    val orderMethod: OrderMethod,
    val paymentKey: String?,
    val itemId: Long,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun from(order: Order): DoneOrderEvent = DoneOrderEvent(
            orderMethod = order.orderMethod!!,
            paymentKey = if (order.isNeedPaid()) order.paymentKey else null,
            userId = order.userId!!,
            orderUuid = order.uuid!!,
            itemId = order.itemId,
        )
    }
}
