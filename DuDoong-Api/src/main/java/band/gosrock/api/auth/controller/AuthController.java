package band.gosrock.api.auth.controller;


import band.gosrock.api.auth.model.dto.request.RegisterRequest;
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.OauthUserInfoResponse;
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.api.auth.service.LoginUseCase;
import band.gosrock.api.auth.service.OauthUserInfoUseCase;
import band.gosrock.api.auth.service.RefreshUseCase;
import band.gosrock.api.auth.service.RegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관련 컨트롤러")
public class AuthController {

    private final RegisterUseCase registerUseCase;

    private final LoginUseCase loginUseCase;
    private final RefreshUseCase refreshUseCase;

    private final OauthUserInfoUseCase oauthUserInfoUseCase;

    @Operation(
            summary = "kakao oauth 링크발급",
            description = "kakao 링크를 받아볼수 있습니다.")
    @GetMapping("/oauth/kakao/link")
    public OauthLoginLinkResponse getKakaoOauthLink() {
        return registerUseCase.getKaKaoOauthLink();
    }

    @Operation(
        summary = "code 요청받는 핸들러, 클라이언트가 몰라도 됩니다",
        deprecated = true)
    @GetMapping("/oauth/kakao")
    public TokenAndUserResponse kakaoAuth(@RequestParam("code") String code) {
        return registerUseCase.upsertKakaoOauthUser(code);
    }


    @Operation(
        summary = "회원가입이 가능한지 id token 으로 확인합니다.")
    @GetMapping("/oauth/kakao/register/valid")
    public AvailableRegisterResponse kakaoAuthCheckRegisterValid(
            @RequestParam("id_token") String token) {
        return registerUseCase.checkAvailableRegister(token);
    }

    @Operation(
        summary = "id_token 으로 회원가입을 합니다.")
    @PostMapping("/oauth/kakao/register")
    public TokenAndUserResponse kakaoAuthCheckRegisterValid(
            @RequestParam("id_token") String token,
            @Valid @RequestBody RegisterRequest registerRequest) {
        return registerUseCase.registerUserByOCIDToken(token, registerRequest);
    }

    @Operation(
        summary = "id_token 으로 로그인을 합니다.")
    @PostMapping("/oauth/kakao/login")
    public TokenAndUserResponse kakaoOauthUserLogin(
            @RequestParam("id_token") String token) {
        return loginUseCase.execute(token);
    }

    @Operation(
        summary = "accessToken 으로 oauth user 정보를 가져옵니다.")
    @PostMapping("/oauth/kakao/info")
    public OauthUserInfoResponse kakaoOauthUserInfo(
        @RequestParam("access_token") String accessToken) {
        return oauthUserInfoUseCase.execute(accessToken);
    }

    @Operation(
        summary = "refreshToken 용입니다.")
    @PostMapping("/token/refresh")
    public TokenAndUserResponse tokenRefresh(@RequestParam("token") String code) {
        return refreshUseCase.execute(code);
    }
}
