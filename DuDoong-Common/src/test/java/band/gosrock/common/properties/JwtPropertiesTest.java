package band.gosrock.common.properties;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class JwtPropertiesTest {

    @Autowired
    JwtProperties jwtProperties;

    @Test
    void JWT_환경변수값이_불러와지는지_확인() {
        Long accessExp = jwtProperties.getAccessExp();
        Long refreshExp = jwtProperties.getRefreshExp();
        assertEquals(accessExp, 3600);
        assertEquals(refreshExp, 3600);
    }
}
