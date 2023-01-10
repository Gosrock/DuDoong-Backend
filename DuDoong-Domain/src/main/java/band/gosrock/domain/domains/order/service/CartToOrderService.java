package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CartToOrderService {

    private final CartAdaptor cartAdaptor;

    private final OrderAdaptor orderAdaptor;

    @Transactional
    public Order creatOrderWithOutCoupon(Long cartId, Long userId) {
        Cart cart = cartAdaptor.queryCart(cartId, userId);
        Order order = Order.createOrder(userId, cart);
        order.calculatePaymentInfo();
        return orderAdaptor.save(order);
    }
}
