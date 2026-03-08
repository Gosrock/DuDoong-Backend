package band.gosrock.infrastructure.outer.api.oauth.config

import band.gosrock.common.exception.DuDoongDynamicException
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoKauthErrorResponse
import band.gosrock.infrastructure.outer.api.oauth.exception.KakaoKauthErrorCode
import feign.Response
import feign.codec.ErrorDecoder

class KauthErrorDecoder : ErrorDecoder {
    override fun decode(methodKey: String, response: Response): Exception {
        val body = KakaoKauthErrorResponse.from(response)
        return try {
            val errorCode = KakaoKauthErrorCode.valueOf(body.errorCode ?: "")
            val errorReason = errorCode.getErrorReason()
            throw DuDoongDynamicException(errorReason.status, errorReason.code, errorReason.reason)
        } catch (e: IllegalArgumentException) {
            val fallback = KakaoKauthErrorCode.KOE_INVALID_REQUEST
            val errorReason = fallback.getErrorReason()
            throw DuDoongDynamicException(errorReason.status, errorReason.code, errorReason.reason)
        }
    }
}
