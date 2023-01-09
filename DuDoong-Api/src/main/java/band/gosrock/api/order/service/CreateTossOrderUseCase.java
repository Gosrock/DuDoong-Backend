package band.gosrock.api.order.service;


import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class CreateTossOrderUseCase {

    private final PaymentsCreateClient paymentsCreateClient;

    public PaymentsResponse execute(String orderId) {
        CreatePaymentsRequest createPaymentsRequest =
                CreatePaymentsRequest.builder()
                        .method("카드")
                        .orderName("고스락 2023년 3월 공연")
                        .orderId(orderId)
                        .failUrl("http://localhost:8080/failurl")
                        .successUrl("http://localhost:8080/successUrl")
                        .amount(Money.wons(15000L).longValue())
                        .build();
        return paymentsCreateClient.execute(createPaymentsRequest);
    }
}
