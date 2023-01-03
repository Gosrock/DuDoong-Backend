package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.common.properties.TossPaymentsProperties;
import band.gosrock.infrastructure.config.redis.AutoConfigureTestFeign;
import band.gosrock.infrastructure.outer.api.tossPayments.config.FeignTossConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CreatePaymentsRequest;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.PaymentsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;


//@AutoConfigureTestFeign
@ActiveProfiles("common")
@SpringBootTest(classes = {FeignTossConfig.class, DuDoongCommonApplication.class})
class TossPaymentsClientTest {
    @Autowired
    TossPaymentsClient tossPaymentsClient;
    @Autowired
    TossPaymentsProperties tossPaymentsProperties;
    @Test
    public void 요청테스트(){
        CreatePaymentsRequest build = CreatePaymentsRequest.builder()
            .successUrl("http://localhost:8080/return-url")
            .failUrl("http://localhost:8080/failurl-url")
            .amount(10000L)
            .orderId("abcd1234")
            .orderName("주문1").build();

        PaymentsResponse payments = tossPaymentsClient.createPayments(build);

    }
}