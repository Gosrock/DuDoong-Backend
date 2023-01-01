package band.gosrock.infrastructure.outer.api;


import band.gosrock.api.auth.model.dto.KakaoInformationResponse;
import band.gosrock.api.auth.port.KakaoInfoPort;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KakaoInfoClient", url = "https://kapi.kakao.com")
public interface KakaoInfoAdaptor extends KakaoInfoPort {

    @GetMapping("/v2/user/me")
    KakaoInformationResponse kakaoUserInfo(@RequestHeader("Authorization") String accessToken);

    @PostMapping("/v1/user/unlink")
    void unlinkUser(@RequestHeader("Authorization") String accessToken);
}
