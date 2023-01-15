package band.gosrock.domain.domains.order.exception;

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
public enum OrderErrorCode implements BaseErrorCode {
    @ExplainError("asdfasdfasdfasdfasfasdf")
    ORDER_NOT_MINE(BAD_REQUEST, "Order-400-1", "Order Not MINE"),
    ORDER_NOT_VALID(BAD_REQUEST, "Order-400-2", "Order Not Valid"),
    ORDER_NOT_PENDING(BAD_REQUEST, "Order-400-3", "Order Status Not Pending"),
    ORDER_NOT_SUPPORTED_METHOD(BAD_REQUEST, "Order-400-4", "Order Method Not Supported"),
    ORDER_CANNOT_CANCEL(BAD_REQUEST, "Order-400-5", "주문을 취소할 수 없는 상태입니다."),
    ORDER_CANNOT_REFUND(BAD_REQUEST, "Order-400-6", "주문을 환불할 수 없는 상태입니다."),
    ORDER_NOT_FOUND(NOT_FOUND, "Order-404-1", "Order Not Found."),
    ORDER_LINE_NOT_FOUND(NOT_FOUND, "Order-404-2", "Order Line Not Fount");

    private Integer status;
    private String code;
    private String reason;

    @Override
    public ErrorReason getErrorReason() {
        return band.gosrock.common.dto.ErrorReason.builder()
                .reason(reason)
                .code(code)
                .status(status)
                .build();
    }

    @Override
    public String getExplainError() throws NoSuchFieldException {
        Field field = this.getClass().getField(this.name());
        ExplainError annotation = field.getAnnotation(ExplainError.class);
        return Objects.nonNull(annotation) ? annotation.value() : this.getReason();
    }
}
