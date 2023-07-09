package band.gosrock.infrastructure.outer.api.tossPayments.config;


import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCancelErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

public class PaymentCancelErrorDecoder implements ErrorDecoder {
    private static final long PERIOD = 100L;
    private static final long MAX_PERIOD = TimeUnit.SECONDS.toMillis(3L);
    private static final int MAX_ATTEMPTS = 3;

    @Bean
    Retryer.Default retryer() {
        return new Retryer.Default(PERIOD, MAX_PERIOD, MAX_ATTEMPTS);
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        TossPaymentsErrorDto body = TossPaymentsErrorDto.from(response);
        try {
            final PaymentsCancelErrorCode paymentsCancelErrorCode =
                    PaymentsCancelErrorCode.valueOf(body.getCode());
            final ErrorReason errorReason = paymentsCancelErrorCode.getErrorReason();
            final Request request = response.request();
            if (HttpStatus.valueOf(body.getCode()).is5xxServerError()) {
                throw new RetryableException(
                        errorReason.getStatus(),
                        errorReason.getReason(),
                        request.httpMethod(),
                        new Date(),
                        request);
            }
            throw new DuDoongDynamicException(
                    errorReason.getStatus(), errorReason.getCode(), errorReason.getReason());
        } catch (IllegalArgumentException e) {
            throw PaymentsUnHandleException.EXCEPTION;
        }
    }
}
