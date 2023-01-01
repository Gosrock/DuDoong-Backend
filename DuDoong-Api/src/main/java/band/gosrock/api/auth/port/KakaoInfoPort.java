package band.gosrock.api.auth.port;


import band.gosrock.api.auth.model.dto.KakaoInformationResponse;
import band.gosrock.common.annotation.Port;

@Port
public interface KakaoInfoPort {
    KakaoInformationResponse kakaoUserInfo(String accessToken);

    void unlinkUser(String accessToken);
}
