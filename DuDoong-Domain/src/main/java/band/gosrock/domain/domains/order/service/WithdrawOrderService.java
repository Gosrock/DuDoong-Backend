package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class WithdrawOrderService {

    private final OrderAdaptor orderAdaptor;
    private final WithdrawPaymentService withdrawPaymentService;

    @RedissonLock(LockName = "결제취소", identifier = "orderUuid")
    public String cancelOrder(String orderUuid, Long userId) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        // TODO : 관리자 권환으로 치환.
        order.validOwner(userId);
        order.cancel();
        return orderUuid;
    }

    @RedissonLock(LockName = "결제취소", identifier = "orderUuid")
    public String refundOrder(String orderUuid, Long userId) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.validOwner(userId);
        order.refund();
        return orderUuid;
    }
}
