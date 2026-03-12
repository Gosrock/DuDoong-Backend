package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor
import band.gosrock.domain.domains.order.domain.Order
import band.gosrock.domain.domains.order.domain.validator.OrderValidator
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor
import band.gosrock.domain.domains.ticket_item.domain.TicketPayType
import org.springframework.transaction.annotation.Transactional

@DomainService
@Transactional(readOnly = true)
class OrderFactory(
    private val cartAdaptor: CartAdaptor,
    private val itemAdaptor: TicketItemAdaptor,
    private val issuedCouponAdaptor: IssuedCouponAdaptor,
    private val orderValidator: OrderValidator,
) {
    fun createNormalOrder(cartId: Long, userId: Long): Order {
        val cart = cartAdaptor.queryCart(cartId, userId)
        val ticketItem = itemAdaptor.queryTicketItem(cart.getItemId())
        val payType = ticketItem.payType
        return when {
            payType == TicketPayType.DUDOONG_TICKET -> Order.createApproveOrder(userId, cart, ticketItem, orderValidator)
            payType == TicketPayType.FREE_TICKET && ticketItem.isFCFS() -> Order.createPaymentOrder(userId, cart, ticketItem, orderValidator)
            payType == TicketPayType.FREE_TICKET -> Order.createApproveOrder(userId, cart, ticketItem, orderValidator)
            else -> Order.createPaymentOrder(userId, cart, ticketItem, orderValidator)
        }
    }

    fun createCouponOrder(cartId: Long, userId: Long, couponId: Long): Order {
        val coupon = issuedCouponAdaptor.query(couponId)
        val cart = cartAdaptor.queryCart(cartId, userId)
        val ticketItem = itemAdaptor.queryTicketItem(cart.getItemId())
        return Order.createPaymentOrderWithCoupon(userId, cart, ticketItem, coupon, orderValidator)
    }
}
