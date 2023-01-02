package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.model.dto.response.AvailableRegisterResponse;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.common.dto.OIDCDecodePayload;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.OauthInfo;
import band.gosrock.domain.domains.user.domain.OauthProvider;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import band.gosrock.domain.domains.user.service.UserDomainService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RegisterUseCase {

    private final KakaoOauthHelper kakaoOauthHelper;
    private final UserDomainService userDomainService;
    private final TokenGenerateHelper tokenGenerateHelper;

    private final UserAdaptor userAdaptor;

    public OauthLoginLinkResponse getKaKaoOauthLink() {
        return new OauthLoginLinkResponse(kakaoOauthHelper.getKaKaoOauthLink());
    }

    /**
     * 개발용 회원가입 upsert 입니다.
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

    public AvailableRegisterResponse checkAvailableRegister(
        String idToken) {
        OIDCDecodePayload oidcDecodePayload = kakaoOauthHelper.getOIDCDecodePayload(idToken);
        Boolean isRegistered = !checkUserCanRegister(oidcDecodePayload);

        return new AvailableRegisterResponse(isRegistered);
    }

    private Boolean checkUserCanRegister(
        OIDCDecodePayload oidcDecodePayload) {
        OauthInfo oauthInfo = OauthInfo.builder().provider(OauthProvider.KAKAO)
            .oid(oidcDecodePayload.getAud()).build();
        return userAdaptor.exist(oauthInfo);
    }
}
