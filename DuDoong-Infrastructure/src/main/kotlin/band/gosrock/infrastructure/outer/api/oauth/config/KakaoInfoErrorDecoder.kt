package band.gosrock.infrastructure.outer.api.oauth.config

import band.gosrock.common.exception.OtherServerBadRequestException
import band.gosrock.common.exception.OtherServerExpiredTokenException
import band.gosrock.common.exception.OtherServerForbiddenException
import band.gosrock.common.exception.OtherServerUnauthorizedException
import feign.FeignException
import feign.Response
import feign.codec.ErrorDecoder

class KakaoInfoErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        if (response.status() >= 400) {
            when (response.status()) {
                401 -> throw OtherServerUnauthorizedException.EXCEPTION
                403 -> throw OtherServerForbiddenException.EXCEPTION
                419 -> throw OtherServerExpiredTokenException.EXCEPTION
                else -> throw OtherServerBadRequestException.EXCEPTION
            }
        }
        return FeignException.errorStatus(methodKey, response)
    }
}
