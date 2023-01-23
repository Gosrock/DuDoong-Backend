package band.gosrock.api.host.model.dto.request;


import band.gosrock.common.annotation.Enum;
import band.gosrock.domain.domains.host.domain.HostRole;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 호스트 슬랙 알람 URL 수정 요청 DTO */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHostUserRoleRequest {
    @Schema(defaultValue = "1", description = "호스트 유저 아이디")
    @Positive(message = "올바른 유저 고유 아이디를 입력해주세요")
    private Long userId;

    @Schema(defaultValue = "HOST", description = "호스트 유저 역할")
    @Enum(enumClass = HostRole.class, message = "HOST 또는 SUPER_HOST 만 허용됩니다")
    private HostRole role;
}
