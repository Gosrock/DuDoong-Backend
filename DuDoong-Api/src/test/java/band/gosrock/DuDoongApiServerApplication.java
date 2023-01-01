package band.gosrock;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@ComponentScan(basePackages= "band.gosrock")
@SpringBootTest
class DuDoongApiServerApplication {
    @Test
    void contextLoads() {}
}
