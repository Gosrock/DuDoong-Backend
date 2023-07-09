package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;

import band.gosrock.infrastructure.InfraIntegrateProfileResolver;
import band.gosrock.infrastructure.InfraIntegrateTestConfig;
import band.gosrock.infrastructure.outer.api.tossPayments.dto.request.CancelPaymentsRequest;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.spec.internal.HttpStatus;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@ContextConfiguration
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        classes = InfraIntegrateTestConfig.class)
@AutoConfigureWireMock(port = 0)
@ActiveProfiles(resolver = InfraIntegrateProfileResolver.class)
@TestPropertySource(properties = {"feign.toss.url=http://localhost:${wiremock.server.port}"})
class PaymentsCancelClientTest {
    private static final String IDEMPOTENCY_KEY = "idempotency-key";
    private static final String PAYMENT_KEY = "1234";
    private static final CancelPaymentsRequest REQUEST =
            CancelPaymentsRequest.builder().cancelReason("test").build();
    @Autowired private PaymentsCancelClient paymentsCancelClient;

    @Test
    public void 주문취소_실패시_멱등성_테스트() {
        final String URL = "/v1/payments/" + PAYMENT_KEY + "/cancel";
        // given
        stubFor(
                post(urlEqualTo(URL))
                        .willReturn(aResponse().withStatus(HttpStatus.SERVICE_UNAVAILABLE)));

        // when
        Throwable exception =
                catchThrowable(
                        () -> paymentsCancelClient.execute(IDEMPOTENCY_KEY, PAYMENT_KEY, REQUEST));

        // then
        assertThat(exception).isInstanceOf(RetryableException.class);
        verify(3, postRequestedFor(urlEqualTo(URL)));
    }
}
