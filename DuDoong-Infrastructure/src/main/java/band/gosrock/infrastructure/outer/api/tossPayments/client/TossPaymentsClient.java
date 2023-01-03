package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.outer.api.tossPayments.config.TossHeaderConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(
        name = "tossCreatePayments",
        url = "https://api.tosspayments.com",
        configuration = TossHeaderConfig.class)
public interface TossPaymentsClient {

    @PostMapping("/v1/payments")
    void createPayments();
}
