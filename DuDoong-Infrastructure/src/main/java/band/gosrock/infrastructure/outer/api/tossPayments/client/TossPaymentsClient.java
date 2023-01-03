package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.FeignTossConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "tossCreatePayments",
        url = "https://api.tosspayments.com",
        configuration = FeignTossConfig.class,
        decode404 = true)
public interface TossPaymentsClient {

    @PostMapping(path = "/v1/payments", consumes = MediaType.APPLICATION_JSON_VALUE)
    PaymentsResponse createPayments(@RequestBody CreatePaymentsRequest createPaymentsRequest);

    @PostMapping("/v1/payments/confirm")
    PaymentsResponse confirmPayments(@RequestBody ConfirmPaymentsRequest confirmPaymentsRequest);

    // TODO : 멱등키 구현
    @PostMapping("/v1/payments/{paymentKey}/cancel")
    PaymentsResponse cancelPayments(@PathVariable("paymentKey") String paymentKey, @RequestBody
        CancelPaymentsRequest cancelPaymentsRequest);

    @GetMapping("/v1/payments/orders/{orderId}")
    PaymentsResponse getTransactionByOrderId(@PathVariable("orderId") String orderId);

    @GetMapping("/v1/payments/{paymentKey}")
    PaymentsResponse getTransactionByPaymentKey(@PathVariable("paymentKey") String paymentKey);
}
