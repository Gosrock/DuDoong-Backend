package band.gosrock.domain.domains.cart.exception;

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
public enum CartErrorCode implements BaseErrorCode {
    @ExplainError("id로 카트를 찾을 때 못 찾으면 발생하는 오류")
    CART_NOT_FOUND(NOT_FOUND, "Cart_404_1", "장바구니를 찾을 수 없습니다."),

    @ExplainError("한 장바구니엔 관련된 한 아이템만 올수 있음")
    CART_INVALID_ITEM_KIND_POLICY(BAD_REQUEST, "Cart_400_1", "장바구니에 아이템을 담는 정책을 위반하였습니다."),
    CART_INVALID_OPTION_ANSWER(BAD_REQUEST, "Cart_400_2", "옵션을 잘못 응답 하였습니다."),
    CART_LINE_NOT_FOUND(BAD_REQUEST, "Cart_400_3", "장바구니 안에 상품을 찾을 수 없습니다.");
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
