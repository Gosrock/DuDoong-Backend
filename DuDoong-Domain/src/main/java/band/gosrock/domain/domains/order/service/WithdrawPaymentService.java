package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCancelClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@DomainService
@RequiredArgsConstructor
@Slf4j
public class WithdrawPaymentService {
    private final PaymentsCancelClient paymentsCancelClient;

    public PaymentsResponse execute(String orderUuid, String paymentKey, String reason) {
        log.info("취소처리 " + orderUuid + " : " + paymentKey + reason);
        return paymentsCancelClient.execute(
                orderUuid,
                paymentKey,
                CancelPaymentsRequest.builder().cancelReason(reason).build());
    }
}
