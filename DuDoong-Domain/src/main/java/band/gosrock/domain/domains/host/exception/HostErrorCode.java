package band.gosrock.domain.domains.host.exception;

import static band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST;
import static band.gosrock.common.consts.DuDoongStatic.NOT_FOUND;

import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.BaseErrorCode;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum HostErrorCode implements BaseErrorCode {
    NOT_MANAGER_HOST(BAD_REQUEST, "HOST_400_1", "매니저 권한이 없는 유저입니다."),
    FORBIDDEN_HOST(BAD_REQUEST, "HOST_400_2", "해당 호스트에 대한 접근 권한이 없습니다."),
    ALREADY_JOINED_HOST(BAD_REQUEST, "HOST_400_3", "이미 가입되어 있는 유저입니다."),
    NOT_MASTER_HOST(BAD_REQUEST, "HOST_400_4", "마스터 호스트 권한이 없는 유저입니다."),
    CANNOT_MODIFY_MASTER_HOST_ROLE(BAD_REQUEST, "HOST_400_5", "마스터 호스트의 권한은 변경할 수 없습니다."),
    NOT_ACCEPTED_HOST(BAD_REQUEST, "HOST_400_6", "아직 초대를 수락하지 않은 유저입니다."),
    HOST_NOT_FOUND(NOT_FOUND, "Host_404_1", "해당 호스트를 찾을 수 없습니다."),
    HOST_USER_NOT_FOUND(NOT_FOUND, "HOST_404_2", "가입된 호스트 유저가 아닙니다."),
    NOT_PARTNER_HOST(BAD_REQUEST, "HOST_400_7", "파트너 호스트만 사용할 수 있는 기능입니다. 제휴 신청을 해주세요.");

    private Integer status;
    private String code;
    private String reason;

    @Override
    public ErrorReason getErrorReason() {
        return ErrorReason.builder().reason(reason).code(code).status(status).build();
    }

    @Override
    public String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(this.name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : this.getReason();
    }
}
