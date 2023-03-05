package band.gosrock.api.auth.service.helper;


import band.gosrock.api.auth.model.dto.response.TokenAndUserResponse;
import band.gosrock.common.annotation.Helper;
import band.gosrock.common.helper.SpringEnvironmentHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

@Helper
@RequiredArgsConstructor
public class CookieHelper {
    private final SpringEnvironmentHelper springEnvironmentHelper;

    public HttpHeaders getTokenCookies(TokenAndUserResponse tokenAndUserResponse) {
        String sameSite = "None";

        if (springEnvironmentHelper.isProdProfile()) {
            sameSite = "Strict";
        }

        ResponseCookie accessToken =
                ResponseCookie.from("accessToken", tokenAndUserResponse.getAccessToken())
                        .path("/")
                        .maxAge(tokenAndUserResponse.getAccessTokenAge())
                        .sameSite(sameSite)
                        //                        .httpOnly(true)
                        .secure(true)
                        .build();
        ResponseCookie refreshToken =
                ResponseCookie.from("refreshToken", tokenAndUserResponse.getRefreshToken())
                        .path("/")
                        .maxAge(tokenAndUserResponse.getRefreshTokenAge())
                        .sameSite(sameSite)
                        //                        .httpOnly(true)
                        .secure(true)
                        .build();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
        return httpHeaders;
    }

    public HttpHeaders deleteCookies() {
        String sameSite = "None";

        if (springEnvironmentHelper.isProdProfile()) {
            sameSite = "Strict";
        }

        ResponseCookie accessToken =
                ResponseCookie.from("accessToken", null)
                        .path("/")
                        .maxAge(0)
                        .sameSite(sameSite)
                        //                        .httpOnly(true)
                        .secure(true)
                        .build();
        ResponseCookie refreshToken =
                ResponseCookie.from("refreshToken", null)
                        .path("/")
                        .maxAge(0)
                        .sameSite(sameSite)
                        //                        .httpOnly(true)
                        .secure(true)
                        .build();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add(HttpHeaders.SET_COOKIE, accessToken.toString());
        httpHeaders.add(HttpHeaders.SET_COOKIE, refreshToken.toString());
        return httpHeaders;
    }
}
