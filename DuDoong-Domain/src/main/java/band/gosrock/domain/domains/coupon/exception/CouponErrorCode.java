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
    DUPLICATE_COUPON_CODE(BAD_REQUEST, "Coupon_400_1", "동일한 쿠폰 코드가 이미 존재합니다."),
    WRONG_DISCOUNT_AMOUNT(BAD_REQUEST, "Coupon_400_2", "정률 할인은 100 이하 퍼센트만 할인 가능합니다."),
    NOT_FOUND_COUPON_CAMPAIGN(BAD_REQUEST, "Coupon_400_3", "존재하지 않는 쿠폰 캠페인입니다."),
    ALREADY_ISSUED_COUPON(BAD_REQUEST, "Coupon_400_4", "이미 발급된 쿠폰입니다."),
    NO_COUPON_STOCK_LEFT(BAD_REQUEST, "Coupon_400_5", "쿠폰이 모두 소진됐습니다.");
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
