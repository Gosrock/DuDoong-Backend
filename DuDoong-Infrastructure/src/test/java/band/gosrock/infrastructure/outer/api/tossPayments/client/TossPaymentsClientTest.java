package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.common.properties.TossPaymentsProperties;
import band.gosrock.infrastructure.config.feign.FeignConfig;
import band.gosrock.infrastructure.config.redis.AutoConfigureTestFeign;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;


@AutoConfigureTestFeign
@ActiveProfiles("common")
@SpringBootTest(classes = {FeignConfig.class, DuDoongCommonApplication.class})
class TossPaymentsClientTest {
    @Autowired
    TossPaymentsClient tossPaymentsClient;
    @Autowired
    TossPaymentsProperties tossPaymentsProperties;
    @Test
    public void 요청테스트(){

    }
}