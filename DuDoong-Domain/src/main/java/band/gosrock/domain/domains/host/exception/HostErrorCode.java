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
    HOST_NOT_FOUND(NOT_FOUND, "Host_404_1", "해당 호스트를 찾을 수 없습니다."),
    NOT_SUPER_HOST(BAD_REQUEST, "HOST_400_1", "슈퍼 호스트 권한이 없는 유저입니다");

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
