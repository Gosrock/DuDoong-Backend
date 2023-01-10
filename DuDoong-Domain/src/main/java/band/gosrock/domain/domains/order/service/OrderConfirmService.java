package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.PaymentMethod;
import band.gosrock.domain.domains.order.exception.InvalidOrderException;
import band.gosrock.domain.domains.order.exception.NotSupportedOrderMethodException;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCancelClient;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsConfirmClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderConfirmService {
    private final PaymentsConfirmClient paymentsConfirmClient;

    private final PaymentsCancelClient paymentsCancelClient;
    private final OrderAdaptor orderAdaptor;

    @RedissonLock(
            LockName = "주문승인",
            identifier = "orderId",
            paramClassType = ConfirmPaymentsRequest.class)
    public Long execute(ConfirmPaymentsRequest confirmPaymentsRequest, Long currentUserId) {
        Order order = orderAdaptor.findByOrderUuid(confirmPaymentsRequest.getOrderId());

        order.confirmPaymentOrder(currentUserId, Money.wons(confirmPaymentsRequest.getAmount()));
        // 결제 승인요청
        PaymentsResponse paymentsResponse = paymentsConfirmClient.execute(confirmPaymentsRequest);
        // TODO : 이넘화 예정
        // TODO : 요청 보내고 난뒤에 도메인 로직 내부에서 실패하면 결제 강제 취소 로직 AOP로 개발 예정
        try {
            // 실제 거래된 금액이 다를때
            if (!paymentsResponse
                    .getTotalAmount()
                    .equals(order.getTotalPaymentPrice().longValue())) {
                throw InvalidOrderException.EXCEPTION;
            }
            // 수정필요
            LocalDateTime approveAt = paymentsResponse.getApprovedAt().toLocalDateTime();
            Money vat = Money.wons(paymentsResponse.getVat());
            PaymentMethod paymentMethod;
            if (paymentsResponse.getMethod().equals("카드")) {
                paymentMethod = PaymentMethod.CARD;
            } else if (paymentsResponse.getMethod().equals("간편결제")) {
                paymentMethod = PaymentMethod.EASYPAY;
            } else {
                throw NotSupportedOrderMethodException.EXCEPTION;
            }
            // 결제 후처리 정보 업데이트
            order.updatePaymentInfo(approveAt, paymentMethod, vat);
            // org.hibernate.LazyInitializationException: could not initialize proxy
            // 분산락 안쪽에 트랜잭션이 이미 커밋된후에 세션이 닫혀버림..
            // 커맨드 치고. 위에서 한번더 찾는게 맞을듯
            orderAdaptor.save(order);
            return order.getId();
        } catch (Exception e) {
            // 내부오류시 결제 강제 취소
            CancelPaymentsRequest cancelPaymentsRequest =
                    CancelPaymentsRequest.builder().cancelReason("서버 내부 오류").build();
            paymentsCancelClient.execute(
                    confirmPaymentsRequest.getPaymentKey(), cancelPaymentsRequest);
            throw InvalidOrderException.EXCEPTION;
        }
    }
}
