package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.PaymentsCancelConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "PaymentsCancelClient",
        url = "https://api.tosspayments.com",
        configuration = {PaymentsCancelConfig.class})
public interface PaymentsCancelClient {
    // TODO : 멱등키 구현
    @PostMapping("/v1/payments/{paymentKey}/cancel")
    PaymentsResponse execute(
            @PathVariable("paymentKey") String paymentKey,
            @RequestBody CancelPaymentsRequest cancelPaymentsRequest);
}