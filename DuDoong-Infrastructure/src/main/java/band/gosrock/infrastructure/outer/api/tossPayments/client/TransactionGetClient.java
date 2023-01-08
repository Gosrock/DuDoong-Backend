package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.TransactionGetConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(
        name = "PaymentsGetClient",
        url = "https://api.tosspayments.com",
        configuration = {TransactionGetConfig.class})
public interface TransactionGetClient {
    @GetMapping("/v1/payments/orders/{orderId}")
    PaymentsResponse byOrderId(@PathVariable("orderId") String orderId);

    @GetMapping("/v1/payments/{paymentKey}")
    PaymentsResponse byPaymentKey(@PathVariable("paymentKey") String paymentKey);
}
