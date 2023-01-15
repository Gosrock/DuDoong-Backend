package band.gosrock.infrastructure.outer.api.oauth.config;


import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.DuDoongDynamicException;
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoKauthErrorResponse;
import band.gosrock.infrastructure.outer.api.oauth.exception.KakaoKauthErrorCode;
import feign.Response;
import feign.codec.ErrorDecoder;

public class KauthErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        KakaoKauthErrorResponse body = KakaoKauthErrorResponse.from(response);

        try {
            KakaoKauthErrorCode kakaoKauthErrorCode =
                    KakaoKauthErrorCode.valueOf(body.getErrorCode());
            ErrorReason errorReason = kakaoKauthErrorCode.getErrorReason();
            throw new DuDoongDynamicException(
                    errorReason.getStatus(), errorReason.getCode(), errorReason.getReason());
        } catch (IllegalArgumentException e) {
            KakaoKauthErrorCode koeInvalidRequest = KakaoKauthErrorCode.KOE_INVALID_REQUEST;
            ErrorReason errorReason = koeInvalidRequest.getErrorReason();
            throw new DuDoongDynamicException(
                    errorReason.getStatus(), errorReason.getCode(), errorReason.getReason());
        }
    }
}
