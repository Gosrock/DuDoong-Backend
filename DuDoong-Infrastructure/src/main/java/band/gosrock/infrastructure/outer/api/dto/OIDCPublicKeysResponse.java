package band.gosrock.infrastructure.outer.api.dto;


import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class OIDCPublicKeysResponse {
    List<OIDCPublicKeyDto> keys;
}
