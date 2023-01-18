package band.gosrock.domain.domains.coupon.exception;

import static band.gosrock.common.consts.DuDoongStatic.BAD_REQUEST;

import band.gosrock.common.annotation.ExplainError;
import band.gosrock.common.dto.ErrorReason;
import band.gosrock.common.exception.BaseErrorCode;
import java.lang.reflect.Field;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CouponErrorCode implements BaseErrorCode {
    DUPLICATE_COUPON_CODE(BAD_REQUEST, "Coupon_400_1", "동일한 쿠폰 코드가 이미 존재합니다.");
    private final Integer status;
    private final String code;
    private final String reason;

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
