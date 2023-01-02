package band.gosrock.infrastructure.outer.api.client;


import band.gosrock.infrastructure.outer.api.dto.KakaoInformationResponse;
import band.gosrock.infrastructure.outer.api.dto.UnlinkKaKaoTarget;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KakaoInfoClient", url = "https://kapi.kakao.com")
public interface KakaoInfoClient {

    @GetMapping("/v2/user/me")
    KakaoInformationResponse kakaoUserInfo(@RequestHeader("Authorization") String accessToken);

    @PostMapping(path = "/v1/user/unlink", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    void unlinkUser(
            @RequestHeader("Authorization") String adminKey, UnlinkKaKaoTarget unlinkKaKaoTarget);
}
