package band.gosrock.infrastructure.outer.api.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OauthTokenResponse {
    private String accessToken;
    private String refreshToken;
    private String idToken;
}
