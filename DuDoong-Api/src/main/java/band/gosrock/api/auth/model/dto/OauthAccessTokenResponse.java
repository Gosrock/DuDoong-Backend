package band.gosrock.api.auth.model.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OauthAccessTokenResponse {
    private String accessToken;
}
