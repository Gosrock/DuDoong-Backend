package band.gosrock.api.auth.service;


import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.model.dto.response.OauthLoginLinkResponse;
import band.gosrock.api.auth.model.dto.response.RegisterResponse;
import band.gosrock.common.annotation.UseCase;
import band.gosrock.common.jwt.JwtTokenProvider;
import band.gosrock.domain.common.dto.ProfileViewDto;
import band.gosrock.domain.domains.user.adaptor.UserAdaptor;
import band.gosrock.domain.domains.user.domain.Profile;
import band.gosrock.domain.domains.user.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@UseCase
@RequiredArgsConstructor
public class RegisterService {

    private final KakaoService kakaoService;
    private final UserAdaptor userDomainService;

    private final JwtTokenProvider jwtTokenProvider;

    public OauthLoginLinkResponse getKaKaoOauthLink() {
        return new OauthLoginLinkResponse(kakaoService.getKaKaoOauthLink());
    }

    public RegisterResponse upsertKakaoOauthUser(String code) {
        String oauthAccessToken = kakaoService.getOauthAccessToken(code);
        KakaoUserInfoDto oauthUserInfo = kakaoService.getUserInfo(oauthAccessToken);

        Profile profile = oauthUserInfo.toProfile();
        User user = userDomainService.upsertUser(profile, oauthUserInfo.toOauthInfo());

        String accessToken =
                jwtTokenProvider.generateAccessToken(
                        user.getId(), user.getAccountRole().getValue());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return RegisterResponse.builder()
                .userProfile(ProfileViewDto.from(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
