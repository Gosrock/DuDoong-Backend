package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.api.auth.service.helper.KakaoOauthHelper;
import band.gosrock.api.auth.service.helper.TokenGenerateHelper;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class LoginUseCase {
    private final KakaoOauthHelper kakaoOauthHelper;
    private final UserDomainService userDomainService;
    private final TokenGenerateHelper tokenGenerateHelper;

    public TokenAndUserResponse execute(String idToken) {
        OauthInfo oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken);

        User user = userDomainService.loginUser(oauthInfo);
        return tokenGenerateHelper.execute(user);
    }
}
