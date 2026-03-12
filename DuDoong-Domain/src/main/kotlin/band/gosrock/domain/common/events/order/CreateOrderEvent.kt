package band.gosrock.domain.common.events.order

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod

class CreateOrderEvent private constructor(
    val orderUuid: String,
    val userId: Long,
    @get:JvmName("getIsUsingCoupon") val isUsingCoupon: Boolean,
    val orderMethod: OrderMethod,
    val issuedCouponId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun from(order: Order): CreateOrderEvent = CreateOrderEvent(
            userId = order.userId!!,
            orderUuid = order.uuid!!,
            isUsingCoupon = order.hasCoupon(),
            issuedCouponId = order.orderCouponVo.couponId,
            orderMethod = order.orderMethod!!,
        )
    }
}
