package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RegisterUseCase {

    private final KakaoOauthHelper kakaoService;
    private final UserDomainService userDomainService;
    private final TokenGenerateHelper tokenGenerateHelper;

    public OauthLoginLinkResponse getKaKaoOauthLink() {
        return new OauthLoginLinkResponse(kakaoService.getKaKaoOauthLink());
    }

    public TokenAndUserResponse upsertKakaoOauthUser(String code) {
        String oauthAccessToken = kakaoService.getOauthAccessToken(code);
        KakaoUserInfoDto oauthUserInfo = kakaoService.getUserInfo(oauthAccessToken);

        Profile profile = oauthUserInfo.toProfile();
        User user = userDomainService.upsertUser(profile, oauthUserInfo.toOauthInfo());

        return tokenGenerateHelper.execute(user);
    }
}
