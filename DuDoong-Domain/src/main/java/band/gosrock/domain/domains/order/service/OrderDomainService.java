package band.gosrock.domain.domains.order.service;


import band.gosrock.common.annotation.DomainService;
import band.gosrock.domain.common.vo.Money;
import band.gosrock.domain.domains.order.domain.Order;
import band.gosrock.domain.domains.order.domain.OrderLineItem;
import band.gosrock.domain.domains.order.domain.PaymentInfo;
import band.gosrock.domain.domains.order.domain.PaymentMethod;
import band.gosrock.domain.domains.order.repository.OrderRepository;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsConfirmClient;
import band.gosrock.infrastructure.outer.api.tossPayments.client.PaymentsCreateClient;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@DomainService
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderDomainService {

    private final OrderRepository orderRepository;

    private final PaymentsCreateClient paymentsCreateClient;
    private final PaymentsConfirmClient paymentsConfirmClient;

    @Transactional
    public PaymentsResponse creatOrder() {
        PaymentInfo paymentInfo =
                PaymentInfo.builder()
                        .paymentAmount(Money.wons(15000L))
                        .discountAmount(Money.ZERO)
                        .supplyAmount(Money.wons(15000L))
                        .build();

        OrderLineItem orderLineItem =
                OrderLineItem.builder()
                        .itemId(1L)
                        .paymentInfo(paymentInfo)
                        .userId(1L)
                        .productName("고스락 2023년 3월 공연")
                        .quantity(3L)
                        .build();
        Order order =
                Order.createOrder(1L, PaymentMethod.EASYPAY, paymentInfo, List.of(orderLineItem));
        Order save = orderRepository.save(order);
        CreatePaymentsRequest createPaymentsRequest =
                CreatePaymentsRequest.builder()
                        .method("카드")
                        .orderName("고스락 2023년 3월 공연")
                        .orderId(save.getUuid())
                        .failUrl("http://localhost:8080/failurl")
                        .successUrl("http://localhost:8080/successUrl")
                        .amount(Money.wons(15000L).longValue())
                        .build();
        return paymentsCreateClient.execute(createPaymentsRequest);
    }

    public PaymentsResponse confirmOrder(ConfirmPaymentsRequest confirmPaymentsRequest) {
        return paymentsConfirmClient.execute(confirmPaymentsRequest);
    }
}
