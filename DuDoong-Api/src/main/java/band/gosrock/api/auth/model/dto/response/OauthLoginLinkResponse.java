package band.gosrock.api.auth.model.dto.response;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OauthLoginLinkResponse {

    private String link;
}
