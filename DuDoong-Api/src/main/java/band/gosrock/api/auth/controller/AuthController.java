package band.gosrock.api.auth.controller;


import band.gosrock.api.auth.model.dto.request.RegisterRequest;
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.api.auth.service.LoginUseCase;
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

    @Operation(
            summary = "kakao oauth 링크발급 ( 서버 개발용 )",
            description = "kakao 링크를 받아볼수 있습니다.",
            deprecated = true)
    @GetMapping("/oauth/kakao/link")
    public OauthLoginLinkResponse getKakaoOauthLink() {
        return registerUseCase.getKaKaoOauthLink();
    }

    @GetMapping("/oauth/kakao")
    public TokenAndUserResponse kakaoAuth(@RequestParam("code") String code) {
        return registerUseCase.upsertKakaoOauthUser(code);
    }

    @GetMapping("/oauth/kakao/register/valid")
    public AvailableRegisterResponse kakaoAuthCheckRegisterValid(
            @RequestParam("id_token") String token) {
        return registerUseCase.checkAvailableRegister(token);
    }

    @PostMapping("/oauth/kakao/register")
    public TokenAndUserResponse kakaoAuthCheckRegisterValid(
            @RequestParam("id_token") String token,
            @Valid @RequestBody RegisterRequest registerRequest) {
        return registerUseCase.registerUserByOCIDToken(token, registerRequest);
    }

    @PostMapping("/oauth/kakao/login")
    public TokenAndUserResponse kakaoAuthCheckRegisterValid(
            @RequestParam("id_token") String token) {
        return loginUseCase.execute(token);
    }

    @PostMapping("/token/refresh")
    public TokenAndUserResponse tokenRefresh(@RequestParam("token") String code) {
        return refreshUseCase.execute(code);
    }
}
