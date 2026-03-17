package band.gosrock.api.admin.controller

import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.admin.service.AdminGetMeUseCase
import band.gosrock.api.admin.service.AdminLocalDevLoginUseCase
import band.gosrock.api.admin.service.AdminLoginUseCase
import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.RefreshUseCase
import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.common.annotation.DevelopOnlyApi
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/internal-api/v1/auth")
@Tag(name = "Admin Auth")
class AdminAuthController(
    private val adminLoginUseCase: AdminLoginUseCase,
    private val adminLocalDevLoginUseCase: AdminLocalDevLoginUseCase,
    private val adminGetMeUseCase: AdminGetMeUseCase,
    private val refreshUseCase: RefreshUseCase,
    private val cookieHelper: CookieHelper,
) {

    @Operation(summary = "어드민 카카오 로그인 (MANAGER 이상만 허용)")
    @PostMapping("/oauth/kakao/login")
    fun adminKakaoLogin(
        @RequestParam("id_token") token: String,
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = adminLoginUseCase.execute(token)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "어드민 로컬 개발용 로그인 (MANAGER 이상만 허용)", deprecated = true)
    @DevelopOnlyApi
    @PostMapping("/oauth/local/login")
    fun adminLocalDevLogin(
        @Valid @RequestBody registerRequest: RegisterRequest,
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = adminLocalDevLoginUseCase.execute(registerRequest)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "어드민 토큰 갱신")
    @PostMapping("/token/refresh")
    fun adminTokenRefresh(
        @CookieValue(value = "refreshToken", required = false) refreshTokenCookie: String?,
        @RequestParam(value = "token", required = false, defaultValue = "") refreshToken: String,
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = refreshUseCase.execute(refreshTokenCookie ?: refreshToken)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "현재 어드민 유저 정보 조회")
    @SecurityRequirement(name = "admin-token")
    @GetMapping("/me")
    fun getAdminMe(@CurrentUserId userId: Long): AdminUserDetailResponse {
        return adminGetMeUseCase.execute(userId)
    }
}
