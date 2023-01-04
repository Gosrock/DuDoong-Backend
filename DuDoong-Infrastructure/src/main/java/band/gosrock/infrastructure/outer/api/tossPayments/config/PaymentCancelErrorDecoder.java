package band.gosrock.infrastructure.outer.api.tossPayments.config;


import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsCancelErrorCode;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException;
import band.gosrock.infrastructure.outer.api.tossPayments.exception.TossPaymentsErrorDto;
import feign.Response;
import feign.codec.ErrorDecoder;

public class PaymentCancelErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        TossPaymentsErrorDto body = TossPaymentsErrorDto.from(response);
        try {
            PaymentsCancelErrorCode paymentsCancelErrorCode =
                    PaymentsCancelErrorCode.valueOf(body.getCode());
            ErrorReason errorReason = paymentsCancelErrorCode.getErrorReason();
            throw new DuDoongDynamicException(
                    errorReason.getStatus(), errorReason.getCode(), errorReason.getReason());
        } catch (IllegalArgumentException e) {
            throw PaymentsUnHandleException.EXCEPTION;
        }
    }
}
