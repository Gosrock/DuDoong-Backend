package band.gosrock.domain.domains.event.exception;

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
public enum EventErrorCode implements BaseErrorCode {
    EVENT_NOT_FOUND(NOT_FOUND, "Event_404_1", "이벤트를 찾을 수 없습니다."),
    HOST_NOT_AUTH_EVENT(BAD_REQUEST, "Event_400_1", "Host Not Auth Event."),

    EVENT_CANNOT_END_BEFORE_START(BAD_REQUEST, "Event_400_2", "시작 시각은 종료 시각보다 빨라야 합니다."),

    EVENT_URL_NAME_ALREADY_EXIST(BAD_REQUEST, "Event_400_3", "중복된 URL 표시 이름입니다."),
    CANNOT_MODIFY_EVENT_BASIC(BAD_REQUEST, "Event_400_4", "이벤트 기본 정보는 수정할 수 없습니다."),
    EVENT_NOT_OPEN(BAD_REQUEST, "Event_400_5", "아직 오픈되지 않은 이벤트에는 접근할 수 없습니다."),
    EVENT_TICKETING_TIME_IS_PASSED(BAD_REQUEST, "Event_400_6", "이벤트 시작시간이 지나 티켓팅을 할 수 없습니다.");
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
