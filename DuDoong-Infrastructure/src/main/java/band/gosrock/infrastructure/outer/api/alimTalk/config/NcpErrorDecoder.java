package band.gosrock.infrastructure.outer.api.alimTalk.config;


import band.gosrock.common.exception.*;
import feign.FeignException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class NcpErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400) {
            switch (response.status()) {
                case 401 -> throw OtherServerUnauthorizedException.EXCEPTION;
                case 403 -> throw OtherServerForbiddenException.EXCEPTION;
                case 404 -> throw OtherServerNotFoundException.EXCEPTION;
                case 500 -> throw OtherServerInternalSeverErrorException.EXCEPTION;
                default -> throw OtherServerBadRequestException.EXCEPTION;
            }
        }

        return FeignException.errorStatus(methodKey, response);
    }
}
