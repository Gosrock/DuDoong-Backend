package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@DomainService
@Transactional(readOnly = true)
public class OrderFactory {

    private final CartAdaptor cartAdaptor;

    private final TicketItemAdaptor itemAdaptor;

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    private final OrderValidator orderValidator;

    public Order createNormalOrder(Long cartId, Long userId) {
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        TicketItem ticketItem = itemAdaptor.queryTicketItem(cart.getItemId());
        // 결제 주문 생성
        if (ticketItem.isFCFS()) {
            return Order.createPaymentOrder(userId, cart, ticketItem, orderValidator);
        }
        // 승인 주문 생성
        return Order.createApproveOrder(userId, cart, ticketItem, orderValidator);
    }

    public Order createCouponOrder(Long cartId, Long userId, Long couponId) {
        IssuedCoupon coupon = issuedCouponAdaptor.query(couponId);
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        TicketItem ticketItem = itemAdaptor.queryTicketItem(cart.getItemId());
        return Order.createPaymentOrderWithCoupon(userId, cart, ticketItem, coupon, orderValidator);
    }
}
