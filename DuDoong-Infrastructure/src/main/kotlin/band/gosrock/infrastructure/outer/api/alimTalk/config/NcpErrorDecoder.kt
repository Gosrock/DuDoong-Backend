package band.gosrock.infrastructure.outer.api.alimTalk.config

import band.gosrock.common.exception.OtherServerBadRequestException
import band.gosrock.common.exception.OtherServerForbiddenException
import band.gosrock.common.exception.OtherServerInternalSeverErrorException
import band.gosrock.common.exception.OtherServerNotFoundException
import band.gosrock.common.exception.OtherServerUnauthorizedException
import feign.FeignException
import feign.Response
import feign.codec.ErrorDecoder

class NcpErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        if (response.status() >= 400) {
            when (response.status()) {
                401 -> throw OtherServerUnauthorizedException.EXCEPTION
                403 -> throw OtherServerForbiddenException.EXCEPTION
                404 -> throw OtherServerNotFoundException.EXCEPTION
                500 -> throw OtherServerInternalSeverErrorException.EXCEPTION
                else -> throw OtherServerBadRequestException.EXCEPTION
            }
        }
        return FeignException.errorStatus(methodKey, response)
    }
}
