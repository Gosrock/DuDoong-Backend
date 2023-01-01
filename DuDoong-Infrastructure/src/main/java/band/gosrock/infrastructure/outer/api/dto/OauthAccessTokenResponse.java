package band.gosrock.infrastructure.outer.api.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OauthAccessTokenResponse {
    private String accessToken;
}
