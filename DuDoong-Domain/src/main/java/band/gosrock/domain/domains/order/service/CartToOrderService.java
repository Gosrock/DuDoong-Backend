package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.cart.adaptor.CartAdaptor;
import band.gosrock.domain.domains.cart.domain.Cart;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.jaxb.OrderAdapter;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CartToOrderService {

    private final CartAdaptor cartAdaptor;
    private final OrderAdapter orderAdapter;

    @Transactional
    public Order creatOrderWithOutCoupon(Long cartId, Long userId) {

        Cart cart = cartAdaptor.queryCart(cartId, userId);
        //        cart.getCartLineItems().stream().map(cartLineItem -> cartLineItem.)

        //
        //        OrderLineItem orderLineItem =
        //            OrderLineItem.builder()
        //                .itemId(1L)
        //                .paymentInfo(paymentInfo)
        //                .userId(1L)
        //                .productName("고스락 2023년 3월 공연")
        //                .quantity(3L)
        //                .build();
        //        Order order =
        //            Order.createOrder(1L, PaymentMethod.EASYPAY, paymentInfo,
        // List.of(orderLineItem));
        //        return orderRepository.save(order);
        return null;
    }
}
