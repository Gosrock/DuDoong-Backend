package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import band.gosrock.infrastructure.DuDoongInfraApplication;
import band.gosrock.infrastructure.InfraIntegrateProfileResolver;
import band.gosrock.infrastructure.InfraIntegrateTestConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import com.github.tomakehurst.wiremock.junit.WireMockClassRule;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.support.TestPropertySourceUtils;
import static com.github.tomakehurst.wiremock.client.WireMock.get;

@ContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,classes = InfraIntegrateTestConfig.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(resolver = InfraIntegrateProfileResolver.class)
@TestPropertySource(properties = {
    "feign.toss.url=http://localhost:${wiremock.server.port}",
    "spring.thymeleaf.enabled=false"
})
public class WiremockForDocsTests {

    @Autowired
    private SettlementClient settlementClient;

    // Using the WireMock APIs in the normal way:
    @Test
    public void contextLoads() throws Exception {
        // Stubbing WireMock
        stubFor(get(urlPathEqualTo("/v1/settlements")).willReturn(aResponse()
            .withHeader("Content-Type", "application/json").withBody("Hello World!")));

        LocalDate now = LocalDate.now();
        List<SettlementResponse> test = settlementClient.execute(now, now, "test", 1, 10);
//        RandomPortInitializer.wireMockRule
//        assertThat(this.service.go()).isEqualTo("Hello World!");
    }

//    @RegisterExtension
//    public static WireMockClassRule wireMockRule = new WireMockClassRule(
//        wireMockConfig().dynamicPort()
//    );
//
//    public class RandomPortInitializer implements
//        ApplicationContextInitializer<ConfigurableApplicationContext> {
//        @Override
//        public void initialize(ConfigurableApplicationContext applicationContext) {
//
//            // If the next statement is commented out,
//            // Feign will go to google.com instead of localhost
//            TestPropertySourceUtils
//                .addInlinedPropertiesToEnvironment(applicationContext,
//                    "google.url=" + "http://localhost:" + wireMockRule.port()
//                );
//        }
//
//    }




}