package band.gosrock.api.order.service;


import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.order.adaptor.OrderAdaptor;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CreateTossOrderUseCase {

    private final PaymentsCreateClient paymentsCreateClient;

    private final OrderAdaptor orderAdaptor;

    public PaymentsResponse execute(String orderId) {
        Order order = orderAdaptor.findByOrderUuid(orderId);
        CreatePaymentsRequest createPaymentsRequest =
                CreatePaymentsRequest.builder()
                        .method("카드")
                        .orderName(order.getOrderName())
                        .orderId(orderId)
                        .failUrl("http://localhost:8080/failurl")
                        .successUrl("http://localhost:8080/successUrl")
                        .amount(order.getTotalPaymentPrice().longValue())
                        .build();
        return paymentsCreateClient.execute(createPaymentsRequest);
    }
}
