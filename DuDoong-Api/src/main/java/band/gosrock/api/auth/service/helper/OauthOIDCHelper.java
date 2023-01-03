package band.gosrock.api.auth.service.helper;


import band.gosrock.common.annotation.Helper;
import band.gosrock.common.dto.OIDCDecodePayload;
import band.gosrock.common.jwt.JwtOIDCProvider;
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeyDto;
import band.gosrock.infrastructure.outer.api.oauth.dto.OIDCPublicKeysResponse;
import lombok.RequiredArgsConstructor;

@Helper
@RequiredArgsConstructor
public class OauthOIDCHelper {
    private final JwtOIDCProvider jwtOIDCProvider;

    private String getKidFromUnsignedIdToken(String token, String iss, String aud) {
        return jwtOIDCProvider.getKidFromUnsignedTokenHeader(token, iss, aud);
    }

    public OIDCDecodePayload getPayloadFromIdToken(
            String token, String iss, String aud, OIDCPublicKeysResponse oidcPublicKeysResponse) {
        String kid = getKidFromUnsignedIdToken(token, iss, aud);

        OIDCPublicKeyDto oidcPublicKeyDto =
                oidcPublicKeysResponse.getKeys().stream()
                        .filter(o -> o.getKid().equals(kid))
                        .findFirst()
                        .orElseThrow();

        return jwtOIDCProvider.getOIDCTokenBody(
                token, oidcPublicKeyDto.getN(), oidcPublicKeyDto.getE());
    }
}
