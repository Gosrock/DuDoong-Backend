package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CreateOrderService {

    private final OrderFactory orderFactory;
    private final OrderAdaptor orderAdaptor;

    @Transactional
    public String withOutCoupon(Long cartId, Long userId) {
        Order order = orderFactory.createNormalOrder(cartId, userId);
        return orderAdaptor.save(order).getUuid();
    }

    @Transactional
    public String withCoupon(Long cartId, Long userId, Long couponId) {
        Order order = orderFactory.createCouponOrder(cartId, userId, couponId);
        return orderAdaptor.save(order).getUuid();
    }
}
