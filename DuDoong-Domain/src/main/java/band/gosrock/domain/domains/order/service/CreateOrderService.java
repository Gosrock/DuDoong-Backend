package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.coupon.adaptor.IssuedCouponAdaptor;
import band.gosrock.domain.domains.coupon.domain.IssuedCoupon;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreateOrderService {

    private final CartAdaptor cartAdaptor;

    private final OrderAdaptor orderAdaptor;

    private final IssuedCouponAdaptor issuedCouponAdaptor;

    @Transactional
    public Order WithOutCoupon(Long cartId, Long userId) {
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        Order order = Order.create(userId, cart);
        order.calculatePaymentInfo();
        return orderAdaptor.save(order);
    }

    @Transactional
    public Order withCoupon(Long cartId, Long userId, Long couponId) {
        IssuedCoupon coupon = issuedCouponAdaptor.query(couponId);
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        Order order = Order.createWithCoupon(userId, cart, coupon);
        order.calculatePaymentInfo();
        return orderAdaptor.save(order);
    }
}
