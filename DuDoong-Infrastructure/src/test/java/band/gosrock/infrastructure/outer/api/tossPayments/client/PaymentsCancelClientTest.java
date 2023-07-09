package band.gosrock.infrastructure.outer.api.tossPayments.client;


import band.gosrock.infrastructure.InfraIntegrateProfileResolver;
import band.gosrock.infrastructure.InfraIntegrateTestConfig;
import org.springframework.boot.test.context.SpringBootTest;
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
    //    private final static String IDEMPOTENCY_KEY = "idempotency-key";
    //    private final static String PAYMENT_KEY = "1234";
    //    private final static CancelPaymentsRequest REQUEST =
    // CancelPaymentsRequest.builder().cancelReason("test").build();
    //    @Autowired
    //    private PaymentsCancelClient paymentsCancelClient;
    //
    //    @Test
    //    public void 주문취소_실패시_멱등성_테스트() {
    //        final String URL = "/v1/payments/" + PAYMENT_KEY + "/cancel";
    //        // given
    //        stubFor(post(urlEqualTo(URL))
    //                .willReturn(aResponse()
    //                .withStatus(HttpStatus.SERVICE_UNAVAILABLE)));
    //
    //        // when
    //        Throwable exception = catchThrowable(
    //                () -> paymentsCancelClient.execute(IDEMPOTENCY_KEY, PAYMENT_KEY, REQUEST));
    //
    //        // then
    //        assertThat(exception).isInstanceOf(RetryableException.class);
    //        verify(3, postRequestedFor(urlEqualTo(URL)));
    //    }
}
