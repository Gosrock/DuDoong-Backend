package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
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

    // 주문 생성간에 승인 주문일경우 이전 목록 조회해서 티켓 발급 한계 개산해야하므로
    // 동시성 이슈가 있음.
    @RedissonLock(LockName = "주문생성", identifier = "userId")
    public String withOutCoupon(Long cartId, Long userId) {
        Order order = orderFactory.createNormalOrder(cartId, userId);
        return orderAdaptor.save(order).getUuid();
    }

    @RedissonLock(LockName = "주문생성", identifier = "userId")
    public String withCoupon(Long cartId, Long userId, Long couponId) {
        Order order = orderFactory.createCouponOrder(cartId, userId, couponId);
        return orderAdaptor.save(order).getUuid();
    }
}
