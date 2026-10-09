package band.gosrock.api.auth.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/** 토큰을 쿼리스트링 대신 본문으로 받는다 (#763) */
data class OauthAccessTokenRequest(
    @field:Schema(description = "카카오 access_token")
    val accessToken: String? = null,
)
