package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.infrastructure.InfraIntegrateProfileResolver;
import band.gosrock.infrastructure.InfraIntegrateTestConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.response.SettlementResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.ResourceUtils;

@ContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = InfraIntegrateTestConfig.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(resolver = InfraIntegrateProfileResolver.class)
@TestPropertySource(
        properties = {
            "feign.toss.url=http://localhost:${wiremock.server.port}",
            "spring.thymeleaf.enabled=false"
        })
public class TossSettlementClientTest {

    @Autowired private SettlementClient settlementClient;

    @Test
    public void 정산요청_올바르게_파싱되어야한다() throws IOException {
        Path file = ResourceUtils.getFile("classpath:payload/settlement-response.json").toPath();

        stubFor(
                get(urlPathEqualTo("/v1/settlements"))
                        .willReturn(
                                aResponse()
                                        .withStatus(HttpStatus.OK.value())
                                        .withHeader(
                                                "Content-Type", MediaType.APPLICATION_JSON_VALUE)
                                        .withBody(Files.readAllBytes(file))));
        LocalDate now = LocalDate.now();
        List<SettlementResponse> test = settlementClient.execute(now, now, "test", 1, 10);

        SettlementResponse settlementResponse = test.get(0);
        assertEquals(settlementResponse.getFees().size(), 2);
    }
}
