package band.gosrock.infrastructure.outer.api.oauth.dto

import band.gosrock.infrastructure.outer.api.tossPayments.exception.PaymentsUnHandleException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming
import feign.Response
import java.io.IOException

@JsonNaming(SnakeCaseStrategy::class)
class KakaoKauthErrorResponse {
    var error: String? = null
    var errorCode: String? = null
    var errorDescription: String? = null

    companion object {
        @JvmStatic
        fun from(response: Response): KakaoKauthErrorResponse {
            return try {
                response.body().asInputStream().use { bodyIs ->
                    ObjectMapper().readValue(bodyIs, KakaoKauthErrorResponse::class.java)
                }
            } catch (e: IOException) {
                throw PaymentsUnHandleException.EXCEPTION
            }
        }
    }
}
