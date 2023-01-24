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
public class FreeOrderService {

    private final OrderAdaptor orderAdaptor;

    @RedissonLock(LockName = "주문", identifier = "orderUuid")
    public String execute(String orderUuid) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.freeConfirm();
        return orderUuid;
    }
}
