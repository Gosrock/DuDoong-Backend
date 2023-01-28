package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.ticket_item.adaptor.TicketItemAdaptor;
import band.gosrock.domain.domains.ticket_item.domain.TicketItem;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreateOrderService {

    private final CartAdaptor cartAdaptor;

    private final TicketItemAdaptor itemAdaptor;

    private final OrderAdaptor orderAdaptor;

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    @Transactional
    public String withOutCoupon(Long cartId, Long userId) {
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        TicketItem ticketItem = itemAdaptor.queryTicketItem(cart.getItemId());
        // 결제 주문 생성
        if (ticketItem.isFCFS()) {
            Order paymentOrder = Order.createPaymentOrder(userId, cart, ticketItem);
            return orderAdaptor.save(paymentOrder).getUuid();
        }
        // 승인 주문 생성
        Order approveOrder = Order.createApproveOrder(userId, cart, ticketItem);
        return orderAdaptor.save(approveOrder).getUuid();
    }

    @Transactional
    public String withCoupon(Long cartId, Long userId, Long couponId) {
        IssuedCoupon coupon = issuedCouponAdaptor.query(couponId);
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        TicketItem ticketItem = itemAdaptor.queryTicketItem(cart.getItemId());
        Order order = Order.createPaymentOrderWithCoupon(userId, cart, ticketItem, coupon);
        order.calculatePaymentInfo();
        return orderAdaptor.save(order).getUuid();
    }
}
