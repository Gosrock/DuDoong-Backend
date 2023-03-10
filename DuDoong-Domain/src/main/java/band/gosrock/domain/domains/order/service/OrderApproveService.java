package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderApproveService {

    private final OrderAdaptor orderAdaptor;

    private final OrderValidator orderValidator;

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    public String execute(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        // TODO : 승인 주문 관리자 할당.
        order.approve(orderValidator);
        return orderUuid;
    }
}
