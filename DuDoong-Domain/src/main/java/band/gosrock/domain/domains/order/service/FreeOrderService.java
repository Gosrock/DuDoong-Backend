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
public class FreeOrderService {

    private final OrderAdaptor orderAdaptor;

    private final OrderValidator orderValidator;

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    public String execute(String orderUuid, Long currentUserId) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.freeConfirm(currentUserId, orderValidator);
        return orderUuid;
    }
}
