package band.gosrock.domain.common.events.order

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.OrderMethod
import band.gosrock.domain.domains.order.domain.OrderStatus

class WithDrawOrderEvent private constructor(
    val orderUuid: String,
    val userId: Long,
    val orderMethod: OrderMethod,
    val orderStatus: OrderStatus,
    val isDudoongTicketOrder: Boolean,
    val isRefund: Boolean,
    val paymentKey: String?,
    val itemId: Long,
    @get:JvmName("getIsUsingCoupon") val isUsingCoupon: Boolean,
    val issuedCouponId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun from(order: Order): WithDrawOrderEvent = WithDrawOrderEvent(
            orderMethod = order.orderMethod!!,
            paymentKey = if (order.isNeedPaid()) order.paymentKey else null,
            userId = order.userId!!,
            orderUuid = order.uuid!!,
            orderStatus = order.orderStatus!!,
            itemId = order.itemId,
            isUsingCoupon = order.hasCoupon(),
            issuedCouponId = order.orderCouponVo.couponId,
            isDudoongTicketOrder = order.isDudoongTicketOrder(),
            isRefund = order.orderStatus == OrderStatus.REFUND,
        )
    }
}
