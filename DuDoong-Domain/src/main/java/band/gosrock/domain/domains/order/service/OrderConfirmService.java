package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.aop.redissonLock.RedissonLock;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.PgPaymentInfo;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsConfirmClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderConfirmService {
    private final PaymentsConfirmClient paymentsConfirmClient;

    private final OrderAdaptor orderAdaptor;

    private final WithdrawPaymentService cancelPaymentService;

    @RedissonLock(
            LockName = "주문승인",
            identifier = "orderId",
            paramClassType = ConfirmPaymentsRequest.class)
    public String execute(ConfirmPaymentsRequest confirmPaymentsRequest, Long currentUserId) {
        Order order = orderAdaptor.findByOrderUuid(confirmPaymentsRequest.getOrderId());
        Money paymentWons = Money.wons(confirmPaymentsRequest.getAmount());
        order.validOwner(currentUserId);
        order.validCanConfirmPayment(paymentWons);
        // 결제 승인요청
        PaymentsResponse paymentsResponse = paymentsConfirmClient.execute(confirmPaymentsRequest);
        // TODO : 요청 보내고 난뒤에 도메인 로직 내부에서 실패하면 결제 강제 취소 로직 AOP로 개발 예정
        try {
            Money pgAmount = Money.wons(paymentsResponse.getTotalAmount());
            // 결제 후처리 정보 업데이트
            order.confirmPayment(
                    pgAmount,
                    paymentsResponse.getApprovedAt().toLocalDateTime(),
                    PgPaymentInfo.from(paymentsResponse));
            return order.getUuid();
        } catch (Exception exception) {
            // 내부오류시 결제 강제 취소
            cancelPaymentService.execute(
                    order.getUuid(), confirmPaymentsRequest.getPaymentKey(), "서버 오류로 인한 환불");
            throw exception;
        }
    }
}
