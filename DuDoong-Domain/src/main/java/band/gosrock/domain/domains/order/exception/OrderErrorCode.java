package band.gosrock.domain.domains.order.exception;

import static band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST;
import static band.gosrock.common.consts.DuDoongStatic.NOT_FOUND;

import band.gosrock.common.annotation.ErrorCode;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@ErrorCode
public enum OrderErrorCode implements BaseErrorCode {
    ORDER_NOT_MINE(BAD_REQUEST, "Order-400-1", "Order Not MINE"),
    ORDER_NOT_VALID(BAD_REQUEST, "Order-400-2", "Order Not Valid"),
    ORDER_NOT_PENDING(BAD_REQUEST, "Order-400-3", "Order Status Not Pending"),
    ORDER_NOT_SUPPORTED_METHOD(BAD_REQUEST, "Order-400-4", "Order Method Not Supported"),
    ORDER_CANNOT_CANCEL(BAD_REQUEST, "Order-400-5", "주문을 취소할 수 없는 상태입니다."),
    ORDER_CANNOT_REFUND(BAD_REQUEST, "Order-400-6", "주문을 환불할 수 없는 상태입니다."),
    ORDER_NOT_FOUND(NOT_FOUND, "Order-404-1", "Order Not Found."),
    ORDER_LINE_NOT_FOUND(BAD_REQUEST, "Order-404-2", "Order Line Not Fount");

    private int status;
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
}
