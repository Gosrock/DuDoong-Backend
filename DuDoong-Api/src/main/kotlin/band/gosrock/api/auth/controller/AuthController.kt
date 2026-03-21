package band.gosrock.api.auth.controller

import band.gosrock.api.auth.model.dto.request.RegisterRequest
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse
import band.gosrock.api.auth.model.dto.response.OauthTokenResponse
import band.gosrock.api.auth.model.dto.response.OauthUserInfoResponse
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse
import band.gosrock.api.auth.service.LocalDevLoginUseCase
import band.gosrock.api.auth.service.LoginUseCase
import band.gosrock.api.auth.service.LogoutUseCase
import band.gosrock.api.auth.service.OauthUserInfoUseCase
import band.gosrock.api.auth.service.RefreshUseCase
import band.gosrock.api.auth.service.RegisterUseCase
import band.gosrock.api.auth.service.WithDrawUseCase
import band.gosrock.api.auth.service.helper.CookieHelper
import band.gosrock.api.config.rateLimit.UserRateLimiter
import band.gosrock.common.annotation.CurrentUserId
import band.gosrock.common.annotation.ApiErrorCodeExample
import band.gosrock.common.annotation.DevelopOnlyApi
import band.gosrock.infrastructure.outer.api.oauth.exception.KakaoKauthErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "1-1. [인증]")
class AuthController(
    private val registerUseCase: RegisterUseCase,
    private val loginUseCase: LoginUseCase,
    private val refreshUseCase: RefreshUseCase,
    private val oauthUserInfoUseCase: OauthUserInfoUseCase,
    private val withDrawUseCase: WithDrawUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val cookieHelper: CookieHelper,
    private val rateLimiter: UserRateLimiter,
    private val localDevLoginUseCase: LocalDevLoginUseCase
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @Operation(summary = "kakao oauth 링크발급 (백엔드용 )", description = "kakao 링크를 받아볼수 있습니다.")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/link/test")
    fun getKakaoOauthLinkTest(): OauthLoginLinkResponse =
        registerUseCase.getKaKaoOauthLinkTest()

    @Operation(summary = "kakao oauth 링크발급 (클라이언트용)", description = "kakao 링크를 받아볼수 있습니다.")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/link")
    fun getKakaoOauthLink(
        @RequestHeader(value = "referer", required = false) referer: String,
        @RequestHeader(value = "host", required = false) host: String
    ): OauthLoginLinkResponse {
        // 스테이징, prod 서버에 배포된 클라이언트에 해당
        if (referer.contains(host)) {
            log.info("/oauth/kakao$host")
            val format = "https://$host"
            if (referer.contains("admin")) {
                return registerUseCase.getKaKaoOauthLink("$format/admin")
            }
            return registerUseCase.getKaKaoOauthLink(format)
        } else if (referer.contains("5173")) {
            return registerUseCase.getKaKaoOauthLink("$referer/admin")
        }
        return registerUseCase.getKaKaoOauthLink(referer)
    }

    @Operation(summary = "카카오 code 요청받는 곳입니다. referer,host는 건들이지 말아주세요!안보내셔도됩니다.")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao")
    @ApiErrorCodeExample(KakaoKauthErrorCode::class)
    fun getCredentialFromKaKao(
        @RequestParam("code") code: String,
        @RequestHeader(value = "referer", required = false) referer: String,
        @RequestHeader(value = "host", required = false) host: String
    ): OauthTokenResponse {
        if (referer.contains(host)) {
            log.info("/oauth/kakao$host")
            val format = "https://$host"
            if (referer.contains("admin")) {
                return registerUseCase.getCredentialFromKaKao(code, "$format/admin")
            }
            return registerUseCase.getCredentialFromKaKao(code, format)
        } else if (referer.contains("5173")) {
            return registerUseCase.getCredentialFromKaKao(code, "$referer/admin")
        }
        return registerUseCase.getCredentialFromKaKao(code, referer)
    }

    @Operation(summary = "개발용 회원가입입니다 클라이언트가 몰라도 됩니다.", deprecated = true)
    @Tag(name = "1-2. [카카오]")
    @DevelopOnlyApi
    @GetMapping("/oauth/kakao/develop")
    fun developUserSign(@RequestParam("code") code: String): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = registerUseCase.upsertKakaoOauthUser(code)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "회원가입이 가능한지 id token 으로 확인합니다.")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/register/valid")
    fun kakaoAuthCheckRegisterValid(
        @RequestParam("id_token") token: String
    ): AvailableRegisterResponse =
        registerUseCase.checkAvailableRegister(token)

    @Operation(summary = "id_token 으로 회원가입을 합니다.")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/register")
    fun kakaoAuthRegister(
        @RequestParam("id_token") token: String,
        @Valid @RequestBody registerRequest: RegisterRequest
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = registerUseCase.registerUserByOCIDToken(token, registerRequest)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "id_token 으로 로그인을 합니다.")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/login")
    fun kakaoOauthUserLogin(
        @RequestParam("id_token") token: String
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = loginUseCase.execute(token)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "accessToken 으로 oauth user 정보를 가져옵니다.")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/info")
    fun kakaoOauthUserInfo(
        @RequestParam("access_token") accessToken: String
    ): OauthUserInfoResponse =
        oauthUserInfoUseCase.execute(accessToken)

    @Operation(summary = "refreshToken 용입니다.")
    @PostMapping("/token/refresh")
    fun tokenRefresh(
        request: HttpServletRequest,
        @RequestParam(value = "token", required = false, defaultValue = "") refreshToken: String
    ): ResponseEntity<TokenAndUserResponse> {
        val refreshTokenCookie = cookieHelper.getRefreshTokenFromRequest(request)
        val tokenAndUserResponse = refreshUseCase.execute(refreshTokenCookie ?: refreshToken)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "회원탈퇴를 합니다.")
    @SecurityRequirement(name = "access-token")
    @DeleteMapping("/me")
    fun withDrawUser(@CurrentUserId userId: Long): ResponseEntity<Void> {
        withDrawUseCase.execute(userId)
        return ResponseEntity.ok().headers(cookieHelper.deleteCookies()).body(null)
    }

    @Operation(summary = "로그아웃을 합니다.")
    @SecurityRequirement(name = "access-token")
    @PostMapping("/logout")
    fun logoutUser(@CurrentUserId userId: Long): ResponseEntity<Void> {
        logoutUseCase.execute(userId)
        return ResponseEntity.ok().headers(cookieHelper.deleteCookies()).body(null)
    }

    @Operation(summary = "로컬 개발용 즉시 로그인 (카카오 불필요)", deprecated = true)
    @Tag(name = "1-2. [카카오]")
    @DevelopOnlyApi
    @PostMapping("/oauth/local/login")
    fun localDevLogin(
        @Valid @RequestBody registerRequest: RegisterRequest
    ): ResponseEntity<TokenAndUserResponse> {
        val tokenAndUserResponse = localDevLoginUseCase.execute(registerRequest)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }
}
