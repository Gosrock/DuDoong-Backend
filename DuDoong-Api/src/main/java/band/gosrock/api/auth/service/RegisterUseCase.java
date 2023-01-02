package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.model.dto.request.RegisterRequest;
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

@UseCase
@RequiredArgsConstructor
public class RegisterUseCase {

    private final KakaoOauthHelper kakaoOauthHelper;
    private final UserDomainService userDomainService;
    private final TokenGenerateHelper tokenGenerateHelper;

    public OauthLoginLinkResponse getKaKaoOauthLink() {
        return new OauthLoginLinkResponse(kakaoOauthHelper.getKaKaoOauthLink());
    }

    /**
     * 개발용 회원가입 upsert 입니다.
     *
     * @param code
     * @return
     */
    public TokenAndUserResponse upsertKakaoOauthUser(String code) {
        String oauthAccessToken = kakaoOauthHelper.getOauthAccessToken(code);
        KakaoUserInfoDto oauthUserInfo = kakaoOauthHelper.getUserInfo(oauthAccessToken);

        Profile profile = oauthUserInfo.toProfile();
        User user = userDomainService.upsertUser(profile, oauthUserInfo.toOauthInfo());

        return tokenGenerateHelper.execute(user);
    }

    public AvailableRegisterResponse checkAvailableRegister(String idToken) {
        OauthInfo oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken);
        return new AvailableRegisterResponse(userDomainService.checkUserCanRegister(oauthInfo));
    }

    @Transactional
    public TokenAndUserResponse registerUserByOCIDToken(
            String idToken, RegisterRequest registerUserRequest) {

        OauthInfo oauthInfo = kakaoOauthHelper.getOauthInfoByIdToken(idToken);
        User user = userDomainService.registerUser(registerUserRequest.toProfile(), oauthInfo);

        return tokenGenerateHelper.execute(user);
    }
}
