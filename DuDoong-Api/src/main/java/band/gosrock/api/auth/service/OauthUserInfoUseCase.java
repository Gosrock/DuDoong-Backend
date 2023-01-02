package band.gosrock.api.auth.service;

import band.gosrock.api.auth.model.dto.KakaoUserInfoDto;
import band.gosrock.api.auth.model.dto.response.OauthUserInfoResponse;
import band.gosrock.common.annotation.UseCase;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class OauthUserInfoUseCase {
    private final KakaoOauthHelper kakaoOauthHelper;

    public OauthUserInfoResponse execute(String accessToken){
        KakaoUserInfoDto oauthUserInfo = kakaoOauthHelper.getUserInfo(accessToken);
        return OauthUserInfoResponse.from(oauthUserInfo);
    }
}
