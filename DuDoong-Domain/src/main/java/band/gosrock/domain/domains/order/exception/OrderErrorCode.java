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
    @ExplainError("본인의 주문이 아닐 때 발생하는 오류. 본인의 주문만 상태변경이 가능한 api 들이 있습니다.")
    ORDER_NOT_MINE(BAD_REQUEST, "Order_400_1", "본인의 주문이 아닙니다."),
    @ExplainError("토스 결제 금액과 , 주문금액이 다를 때 등 올바르지 않은 주문 상태를 가질 때 발생하는 오류입니다.")
    ORDER_NOT_VALID(BAD_REQUEST, "Order_400_2", "올바르지 않은 주문입니다."),
    ORDER_NOT_PENDING(BAD_REQUEST, "Order_400_3", "결제,승인 대기중인 주문이 아닙니다."),
    ORDER_NOT_SUPPORTED_METHOD(BAD_REQUEST, "Order_400_4", "지원하지 않는 방식의 주문입니다."),
    ORDER_CANNOT_CANCEL(BAD_REQUEST, "Order_400_5", "주문을 취소할 수 없는 상태입니다."),
    ORDER_CANNOT_REFUND(BAD_REQUEST, "Order_400_6", "주문을 환불할 수 없는 상태입니다."),

    ORDER_NOT_APPROVAL(BAD_REQUEST, "Order_400_7", "승인 주문이 아닙니다."),
    ORDER_NOT_PAYMENT(BAD_REQUEST, "Order_400_8", "결제 주문이 아닙니다."),

    ORDER_NOT_REFUND_DATE(BAD_REQUEST, "Order_400_9", "환불을 할 수 있는 기한을 지났습니다."),
    ORDER_NOT_FOUND(NOT_FOUND, "Order_404_1", "주문을 찾을 수 없습니다."),
    ORDER_LINE_NOT_FOUND(NOT_FOUND, "Order_404_2", "주문 라인을 찾을 수 없습니다."),
    ORDER_NOT_FREE(BAD_REQUEST, "Order_400_10", "무료 주문이 아닙니다."),

    ORDER_LESS_THAN_MINIMUM(BAD_REQUEST, "Order_400_11", "최소 결제금액인 1000원보다 낮은 주문입니다."),
    @ExplainError("한 장바구니엔 관련된 한 아이템만 올수 있음")
    ORDER_INVALID_ITEM_KIND_POLICY(BAD_REQUEST, "Order_400_12", "장바구니에 아이템을 담는 정책을 위반하였습니다."),
    ORDER_OPTION_CHANGED(BAD_REQUEST, "Order_400_13", "주문 과정중 아이템의 옵션이 변화했습니다."),
    CAN_NOT_DELETED_USER_APPROVE(BAD_REQUEST, "Order_400_14", "유저가 탈퇴를 했습니다."),
    APPROVE_WAITING_PURCHASE_LIMIT(
            BAD_REQUEST,
            "Order_400_15",
            "승인 대기중인 주문으로 인해 티켓 최대 구매 가능 횟수를 넘겼습니다." + "이미 신청한 주문이 승인 될 때까지 기다려주세요.");

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
