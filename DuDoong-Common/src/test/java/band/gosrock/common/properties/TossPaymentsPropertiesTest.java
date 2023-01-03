package band.gosrock.common.properties;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("common")
class TossPaymentsPropertiesTest {

    @Autowired
    TossPaymentsProperties tossPaymentsProperties;

    @Test
    void 토스페이먼츠_환경변수_값_확인() {
        String secretKey = tossPaymentsProperties.getSecretKey();
        String mid = tossPaymentsProperties.getMid();
        assertNotNull(secretKey);
        assertNotNull(mid);
    }
}