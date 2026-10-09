package band.gosrock.api.auth.model.dto.request

import io.swagger.v3.oas.annotations.media.Schema

/** 토큰을 쿼리스트링 대신 본문으로 받는다 (#763) */
data class RefreshTokenRequest(
    @field:Schema(description = "재발급할 refreshToken. 쿠키로 보내면 비워 둔다")
    val refreshToken: String? = null,
)
