package band.gosrock.infrastructure.outer.api.oauth.client

import band.gosrock.infrastructure.outer.api.oauth.config.KakaoKauthConfig
import band.gosrock.infrastructure.outer.api.oauth.dto.KakaoTokenResponse
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse
import org.springframework.cache.annotation.Cacheable
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping

@FeignClient(
    name = "KakaoAuthClient",
    url = "https://kauth.kakao.com",
    configuration = [KakaoKauthConfig::class],
)
interface KakaoOauthClient {

    @PostMapping("/oauth/token?grant_type=authorization_code&client_id={CLIENT_ID}&redirect_uri={REDIRECT_URI}&code={CODE}&client_secret={CLIENT_SECRET}")
    fun kakaoAuth(
        @PathVariable("CLIENT_ID") clientId: String,
        @PathVariable("REDIRECT_URI") redirectUri: String,
        @PathVariable("CODE") code: String,
        @PathVariable("CLIENT_SECRET") clientSecret: String,
    ): KakaoTokenResponse

    @Cacheable(cacheNames = ["KakaoOICD"], cacheManager = "oidcCacheManager")
    @GetMapping("/.well-known/jwks.json")
    fun getKakaoOIDCOpenKeys(): OIDCPublicKeysResponse
}
