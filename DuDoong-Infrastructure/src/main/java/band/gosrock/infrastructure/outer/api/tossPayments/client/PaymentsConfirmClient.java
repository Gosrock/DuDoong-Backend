package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.PaymentsConfirmConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.ConfirmPaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "PaymentsConfirmClient",
        url = "https://api.tosspayments.com",
        configuration = {PaymentsConfirmConfig.class})
public interface PaymentsConfirmClient {
    @PostMapping("/v1/payments/confirm")
    PaymentsResponse execute(@RequestBody ConfirmPaymentsRequest confirmPaymentsRequest);
}
