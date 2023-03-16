package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WithdrawOrderService {

    private final OrderAdaptor orderAdaptor;

    private final OrderValidator orderValidator;

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    public String cancelOrder(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.cancel(orderValidator);
        return orderUuid;
    }

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    public String refundOrder(String orderUuid, Long userId) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.refund(userId, orderValidator);
        return orderUuid;
    }
}
