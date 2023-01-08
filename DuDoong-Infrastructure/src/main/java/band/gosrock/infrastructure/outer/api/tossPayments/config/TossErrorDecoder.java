package band.gosrock.infrastructure.outer.api.tossPayments.config;


import band.gosrock.common.exception.OtherServerBadRequestException;
import band.gosrock.common.exception.OtherServerExpiredTokenException;
import band.gosrock.common.exception.OtherServerForbiddenException;
import band.gosrock.common.exception.OtherServerUnauthorizedException;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class TossErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400) {
            switch (response.status()) {
                case 401:
                    throw OtherServerUnauthorizedException.EXCEPTION;
                case 403:
                    throw OtherServerForbiddenException.EXCEPTION;
                case 404:
                    throw OtherServerForbiddenException.EXCEPTION;
                case 419:
                    throw OtherServerExpiredTokenException.EXCEPTION;
                default:
                    throw OtherServerBadRequestException.EXCEPTION;
            }
        }

        return FeignException.errorStatus(methodKey, response);
    }
}
