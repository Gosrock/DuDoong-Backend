package band.gosrock.api.auth.controller

import band.gosrock.api.auth.model.dto.request.IdTokenRequest
import band.gosrock.api.auth.model.dto.request.OauthAccessTokenRequest
import band.gosrock.api.auth.model.dto.request.RefreshTokenRequest
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
import band.gosrock.api.auth.service.helper.KakaoRedirectPolicy
import band.gosrock.api.auth.service.helper.OauthStateHelper
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
import org.springframework.web.bind.MissingServletRequestParameterException
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
    private val localDevLoginUseCase: LocalDevLoginUseCase,
    private val kakaoRedirectPolicy: KakaoRedirectPolicy,
    private val oauthStateHelper: OauthStateHelper,
) {
    private val log = LoggerFactory.getLogger(AuthController::class.java)

    @Operation(summary = "kakao oauth 링크발급 (백엔드용 )", description = "kakao 링크를 받아볼수 있습니다.")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/link/test")
    fun getKakaoOauthLinkTest(): OauthLoginLinkResponse =
        registerUseCase.getKaKaoOauthLinkTest()

    @Operation(
        summary = "kakao oauth 링크발급 (클라이언트용)",
        description = "kakao 링크를 받아볼수 있습니다. redirect_uri 는 서버 허용 목록에서 고르고(Referer 로 관객 앱·호스트 어드민 구분), " +
            "링크에 state 를 붙이며 같은 값을 HttpOnly 쿠키로 내려줍니다. 콜백의 state 를 /oauth/kakao 에 그대로 넘겨 주세요 (#763)."
    )
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/link")
    fun getKakaoOauthLink(
        @RequestHeader(value = "referer", required = false) referer: String?
    ): ResponseEntity<OauthLoginLinkResponse> {
        val state = oauthStateHelper.issue()
        val link = registerUseCase.getKaKaoOauthLink(kakaoRedirectPolicy.resolveBase(referer), state)
        return ResponseEntity.ok()
            .headers(oauthStateHelper.stateCookie(state))
            .body(link)
    }

    @Operation(summary = "카카오 code 요청받는 곳입니다. 콜백으로 받은 state 를 함께 보내 주세요. referer 는 건들이지 말아주세요!")
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao")
    @ApiErrorCodeExample(KakaoKauthErrorCode::class)
    fun getCredentialFromKaKao(
        request: HttpServletRequest,
        @RequestParam("code") code: String,
        @RequestParam(value = "state", required = false) state: String?,
        @RequestHeader(value = "referer", required = false) referer: String?
    ): ResponseEntity<OauthTokenResponse> {
        val headers = oauthStateHelper.verify(request, state)
        val tokenResponse = registerUseCase.getCredentialFromKaKao(code, kakaoRedirectPolicy.resolveBase(referer))
        return ResponseEntity.ok().headers(headers).body(tokenResponse)
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

    @Operation(summary = "회원가입이 가능한지 id token 으로 확인합니다. (쿼리스트링 id_token: 전환 기간용, POST 본문 사용)", deprecated = true)
    @Tag(name = "1-2. [카카오]")
    @GetMapping("/oauth/kakao/register/valid")
    fun kakaoAuthCheckRegisterValid(
        @RequestParam("id_token") token: String
    ): AvailableRegisterResponse {
        warnQueryToken("id_token", "/oauth/kakao/register/valid")
        return registerUseCase.checkAvailableRegister(token)
    }

    @Operation(summary = "회원가입이 가능한지 id token 으로 확인합니다. (본문 idToken)")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/register/valid")
    fun kakaoAuthCheckRegisterValidByBody(
        @RequestBody body: IdTokenRequest
    ): AvailableRegisterResponse =
        registerUseCase.checkAvailableRegister(requireToken(body.idToken, "idToken"))

    @Operation(summary = "id_token 으로 회원가입을 합니다. id_token 은 본문 idToken 으로 보내 주세요 (쿼리스트링 id_token 은 전환 기간용)")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/register")
    fun kakaoAuthRegister(
        @RequestParam(value = "id_token", required = false) queryToken: String?,
        @Valid @RequestBody registerRequest: RegisterRequest
    ): ResponseEntity<TokenAndUserResponse> {
        val token = registerRequest.idToken.takeUnless { it.isNullOrBlank() }
            ?: queryToken?.also { warnQueryToken("id_token", "/oauth/kakao/register") }
        val tokenAndUserResponse = registerUseCase.registerUserByOCIDToken(requireToken(token, "id_token"), registerRequest)
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "id_token 으로 로그인을 합니다. id_token 은 본문 idToken 으로 보내 주세요 (쿼리스트링 id_token 은 전환 기간용)")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/login")
    fun kakaoOauthUserLogin(
        @RequestParam(value = "id_token", required = false) queryToken: String?,
        @RequestBody(required = false) body: IdTokenRequest?
    ): ResponseEntity<TokenAndUserResponse> {
        val token = body?.idToken.takeUnless { it.isNullOrBlank() }
            ?: queryToken?.also { warnQueryToken("id_token", "/oauth/kakao/login") }
        val tokenAndUserResponse = loginUseCase.execute(requireToken(token, "id_token"))
        return ResponseEntity.ok()
            .headers(cookieHelper.getTokenCookies(tokenAndUserResponse))
            .body(tokenAndUserResponse)
    }

    @Operation(summary = "accessToken 으로 oauth user 정보를 가져옵니다. 본문 accessToken 으로 보내 주세요 (쿼리스트링 access_token 은 전환 기간용)")
    @Tag(name = "1-2. [카카오]")
    @PostMapping("/oauth/kakao/info")
    fun kakaoOauthUserInfo(
        @RequestParam(value = "access_token", required = false) queryToken: String?,
        @RequestBody(required = false) body: OauthAccessTokenRequest?
    ): OauthUserInfoResponse {
        val token = body?.accessToken.takeUnless { it.isNullOrBlank() }
            ?: queryToken?.also { warnQueryToken("access_token", "/oauth/kakao/info") }
        return oauthUserInfoUseCase.execute(requireToken(token, "access_token"))
    }

    @Operation(summary = "refreshToken 용입니다. 본문 refreshToken 또는 refreshToken 쿠키로 보내 주세요 (쿼리스트링 token 은 전환 기간용)")
    @PostMapping("/token/refresh")
    fun tokenRefresh(
        request: HttpServletRequest,
        @RequestParam(value = "token", required = false) queryToken: String?,
        @RequestBody(required = false) body: RefreshTokenRequest?
    ): ResponseEntity<TokenAndUserResponse> {
        // 우선순위: 본문 → 쿠키 → 쿼리스트링(전환 기간)
        val refreshToken = body?.refreshToken.takeUnless { it.isNullOrBlank() }
            ?: cookieHelper.getRefreshTokenFromRequest(request)
            ?: queryToken?.takeIf { it.isNotEmpty() }?.also { warnQueryToken("token", "/token/refresh") }
            ?: ""
        val tokenAndUserResponse = refreshUseCase.execute(refreshToken)
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
        // 익명(정지·탈퇴 계정 포함)은 userId 0: 쿠키만 지운다
        if (userId != 0L) logoutUseCase.execute(userId)
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

    /** 쿼리스트링 토큰은 전환 기간에만 받는다 (#763). 사용량 파악용 로그 — 토큰 값은 남기지 않는다 */
    private fun warnQueryToken(param: String, path: String) {
        log.warn("[DEPRECATED] 쿼리스트링 토큰 사용 - param={}, path=/api/v1/auth{}", param, path)
    }

    private fun requireToken(token: String?, name: String): String {
        if (token.isNullOrBlank()) throw MissingServletRequestParameterException(name, "String")
        return token
    }
}
