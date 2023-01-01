package io.github.depromeet.knockknockbackend.global.utils.api.client;


import io.github.depromeet.knockknockbackend.global.utils.api.dto.response.KakaoInformationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "KakaoInfoClient", url = "https://kapi.kakao.com")
public interface KakaoInfoClient {

    @GetMapping("/v2/user/me")
    KakaoInformationResponse kakaoUserInfo(@RequestHeader("Authorization") String accessToken);

    @PostMapping("/v1/user/unlink")
    void unlinkUser(@RequestHeader("Authorization") String accessToken);
}
