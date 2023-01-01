package band.gosrock.common.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AccessTokenBodyDto {
    private final String userId;
    private final String role;
}
