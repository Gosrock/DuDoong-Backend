package band.gosrock.domain.user.domain;

import javax.persistence.EnumType;
import javax.persistence.Enumerated;

public class OauthInfo {

    @Enumerated(EnumType.STRING)
    private OauthProvider oauthProvider;

    private String oauthId;
}
