package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
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
        if(order.isMethodPayment()) {
            PaymentsResponse paymentsResponse =
                withdrawPaymentService.execute(
                    order.getUuid(), order.getPaymentKey(), "이벤트 관리자에 의한 취소");
        }
        return orderUuid;
    }

    @RedissonLock(LockName = "결제취소", identifier = "orderUuid")
    public String refundOrder(String orderUuid, Long userId) {
        Order order = orderAdaptor.findByOrderUuid(orderUuid);
        order.validOwner(userId);
        order.refund();
        if(order.isMethodPayment()){
            PaymentsResponse paymentsResponse =
                withdrawPaymentService.execute(
                    order.getUuid(), order.getPaymentKey(), "구매자에의한 환불 요청");
        }
        return orderUuid;
    }
}
