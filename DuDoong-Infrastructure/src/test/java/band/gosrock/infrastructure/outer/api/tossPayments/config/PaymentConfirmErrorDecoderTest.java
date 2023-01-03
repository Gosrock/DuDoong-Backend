package band.gosrock.infrastructure.outer.api.tossPayments.config;

import static org.junit.jupiter.api.Assertions.*;

import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsConfirmErrorCode;
import org.junit.jupiter.api.Test;

class PaymentConfirmErrorDecoderTest {

    @Test
    void 코드가주어졌을때_ErrorCode_로변환할수있어야한다() {
        String code = "ALREADY_PROCESSED_PAYMENT";
        PaymentsConfirmErrorCode paymentsConfirmErrorCode = PaymentsConfirmErrorCode.valueOf(code);

        assertEquals(code, paymentsConfirmErrorCode.name());
    }
}
