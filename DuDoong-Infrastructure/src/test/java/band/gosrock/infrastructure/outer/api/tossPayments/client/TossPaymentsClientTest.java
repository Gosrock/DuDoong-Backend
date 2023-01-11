package band.gosrock.infrastructure.outer.api.tossPayments.client;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.common.DuDoongCommonApplication;
import band.gosrock.infrastructure.config.feign.FeignCommonConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// @AutoConfigureTestFeign
@ActiveProfiles({"common"})
@SpringBootTest(classes = {FeignCommonConfig.class, DuDoongCommonApplication.class})
class TossPaymentsClientTest {
    //        @Autowired
    //        PaymentsCancelClient paymentsCancelClient;
    //        @Autowired PaymentsCreateClient paymentsCreateClient;
    //        @Autowired PaymentsConfirmClient paymentsConfirmClient;
    //        @Autowired TransactionGetClient transactionGetClient;
    //
    //        @Test
    //        public void 결제_요청_테스트() {
    //            CreatePaymentsRequest createPaymentsRequest =
    //                    CreatePaymentsRequest.builder()
    //                            .successUrl("http://localhost:8080/return-url")
    //                            .failUrl("http://localhost:8080/failurl-url")
    //                            .amount(10000L)
    //                            .orderId("abcd1233")
    //                            .method("카드")
    //                            .orderName("주문1")
    //                            .build();
    //
    //            PaymentsResponse createPaymentResponse =
    //                    paymentsCreateClient.execute(createPaymentsRequest);
    //            LocalDateTime localDateTime =
    // createPaymentResponse.getRequestedAt().toLocalDateTime();
    //        }
    //
    //        @Test
    //        public void 결제_승인_테스트() {
    //            ConfirmPaymentsRequest confirmPaymentsRequest =
    //                    ConfirmPaymentsRequest.builder()
    //                            .paymentKey("qKl56WYb7w4vZnjEJeQVxYQd9zBz9rPmOoBN0k12dzgRG9px")
    //                            .amount(10000L)
    //                            .orderId("abcd1233")
    //                            .build();
    //            PaymentsResponse confirmPaymentResponse =
    //                    paymentsConfirmClient.execute(confirmPaymentsRequest);
    //
    //            String orderId = confirmPaymentResponse.getOrderId();
    //            String paymentKey = confirmPaymentResponse.getPaymentKey();
    //        }

    //    @Test
    //    public void 결제_취소_테스트() {
    //        CancelPaymentsRequest cancelPaymentsRequest =
    //                CancelPaymentsRequest.builder().cancelReason("취소 사유").build();
    //
    //        PaymentsResponse paymentsResponse =
    //                paymentsCancelClient.execute(
    //                        "qKl56WYb7w4vZnjEJeQVxYQd9zBz9rPmOoBN0k12dzgRG9px",
    // cancelPaymentsRequest);
    //    }
}
