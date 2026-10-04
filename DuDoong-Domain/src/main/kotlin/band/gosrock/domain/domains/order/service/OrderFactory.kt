package band.gosrock.domain.domains.order.service

import band.gosrock.common.annotation.DomainService
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor
import band.gosrock.domain.domains.cart.domain.Cart
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
    fun createNormalOrder(cartId: Long, userId: Long): Order =
        createNormalOrder(cartAdaptor.queryCart(cartId, userId), userId)

    /**
     * 장바구니(저장 여부 무관)로 주문 생성. 결제 방식 규칙(두둥 → 승인형, 무료 선착순 → 결제형(무료 확정), 무료 승인 → 승인형, 그 외 결제형)은 v1/v2 공통.
     * v2 사용자 주문(#718)은 장바구니를 저장하지 않고(v1 '최근 장바구니'를 덮어쓰지 않도록) 이 메서드를 부른다
     */
    fun createNormalOrder(cart: Cart, userId: Long): Order {
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
