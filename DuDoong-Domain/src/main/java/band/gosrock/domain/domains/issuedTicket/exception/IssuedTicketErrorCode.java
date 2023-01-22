package band.gosrock.domain.domains.issuedTicket.exception;

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
public enum IssuedTicketErrorCode implements BaseErrorCode {
    ISSUED_TICKET_NOT_FOUND(NOT_FOUND, "IssuedTicket_404_1", "IssuedTicket Not Found"),
    ISSUED_TICKET_NOT_MATCHED_USER(
            BAD_REQUEST, "IssuedTicket_400_1", "IssuedTicket User Not Matched"),
    CAN_NOT_CANCEL(BAD_REQUEST, "IssuedTicket_400_2", "티켓을 취소 할 수 있는 상태가 아닙니다."),
    CAN_NOT_CANCEL_ENTRANCE(BAD_REQUEST, "IssuedTicket_400_3", "티켓이 입장 취소 할 수 있는 상태가 아닙니다."),
    CAN_NOT_ENTRANCE(BAD_REQUEST, "IssuedTicket_400_5", "티켓이 입장 할 수 있는 상태가 아닙니다.");

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
