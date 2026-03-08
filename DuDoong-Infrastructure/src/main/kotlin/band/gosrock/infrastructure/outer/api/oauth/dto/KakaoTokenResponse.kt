package band.gosrock.infrastructure.outer.api.oauth.dto

import com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

@JsonNaming(SnakeCaseStrategy::class)
class KakaoTokenResponse {
    var accessToken: String? = null
    var refreshToken: String? = null
    var idToken: String? = null
}
