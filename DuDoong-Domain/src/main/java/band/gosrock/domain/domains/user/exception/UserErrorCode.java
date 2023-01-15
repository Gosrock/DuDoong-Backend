package band.gosrock.domain.domains.user.exception;

import static band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST;
import static band.gosrock.common.consts.DuDoongStatic.FORBIDDEN;
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
public enum UserErrorCode implements BaseErrorCode {
    @ExplainError("회원가입시에 이미 회원가입한 유저일시 발생하는 오류. 회원가입전엔 항상 register valid check 를 해주세요")
    USER_ALREADY_SIGNUP(BAD_REQUEST, "USER_400_1", "이미 회원가입한 유저입니다."),
    @ExplainError("정지 처리된 유저일 경우 밣생하는 오류")
    USER_FORBIDDEN(FORBIDDEN, "USER_403_1", "접근이 제한된 유저입니다."),
    @ExplainError("탈퇴한 유저로 접근하려는 경우")
    USER_ALREADY_DELETED(FORBIDDEN, "USER_403_2", "이미 지워진 유저입니다."),
    @ExplainError("유저 정보를 찾을 수 없는 경우")
    USER_NOT_FOUND(NOT_FOUND, "USER_404_1", "유저 정보를 찾을 수 없습니다.");

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
