package band.gosrock.common.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OIDCDecodePayload {
    /** issuer ex https://kauth.kakao.com */
    private String iss;
    /** client id */
    private String aud;
    /** oauth provider account unique id */
    private String sub;

    private String email;
}
