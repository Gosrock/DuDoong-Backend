package band.gosrock.domain.domains.comment.exception;

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
public enum CommentErrorCode implements BaseErrorCode {
    COMMENT_NOT_FOUND(NOT_FOUND, "Comment_404_1", "응원글을 찾을 수 없습니다."),
    @ExplainError(value = "eventId 경로변수와 commentId가 맞지 않을 때 발생하는 에러입니다.")
    COMMENT_NOT_MATCH_EVENT(BAD_REQUEST, "Comment_400_1", "응원글과 이벤트가 맞지 않습니다."),
    COMMENT_ALREADY_DELETE(BAD_REQUEST, "Comment_400_2", "이미 삭제된 응원글입니다.");


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
