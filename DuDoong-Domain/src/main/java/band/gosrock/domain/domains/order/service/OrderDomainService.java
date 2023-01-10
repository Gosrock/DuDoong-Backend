package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.domains.order.repository.OrderRepository;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsConfirmClient;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderDomainService {

    private final OrderRepository orderRepository;

    private final PaymentsCreateClient paymentsCreateClient;
    private final PaymentsConfirmClient paymentsConfirmClient;

    public PaymentsResponse confirmOrder(ConfirmPaymentsRequest confirmPaymentsRequest) {
        return paymentsConfirmClient.execute(confirmPaymentsRequest);
    }
}
