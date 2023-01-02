package band.gosrock.api.auth.controller;


import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.RegisterResponse;
import band.gosrock.api.auth.service.RegisterUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "인증 관련 컨트롤러")
public class AuthController {

    private final RegisterUseCase registerService;

    @Operation(
            summary = "kakao oauth 링크발급 ( 서버 개발용 )",
            description = "kakao 링크를 받아볼수 있습니다.",
            deprecated = true)
    @GetMapping("/oauth/kakao/link")
    public OauthLoginLinkResponse getKakaoOauthLink() {
        return registerService.getKaKaoOauthLink();
    }

    @GetMapping("/oauth/kakao")
    public RegisterResponse kakaoAuth(@RequestParam("code") String code) {
        return registerService.upsertKakaoOauthUser(code);
    }
}
