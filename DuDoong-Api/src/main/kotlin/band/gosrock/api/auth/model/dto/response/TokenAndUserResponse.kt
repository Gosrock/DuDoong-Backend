package band.gosrock.api.auth.model.dto.response

import band.gosrock.domain.common.dto.ProfileViewDto
import io.swagger.v3.oas.annotations.media.Schema

data class TokenAndUserResponse(
    @Schema(description = "어세스 토큰")
    val accessToken: String,
    val accessTokenAge: Long,
    @Schema(description = "리프레쉬 토큰")
    val refreshToken: String,
    val refreshTokenAge: Long,
    val userProfile: ProfileViewDto
)
