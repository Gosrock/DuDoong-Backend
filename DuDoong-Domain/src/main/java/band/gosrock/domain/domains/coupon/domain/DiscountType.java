package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.common.annotation.EnumClass;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EnumClass
public enum DiscountType {
    // 정액 할인
    AMOUNT("AMOUNT"),
    // 정률 할인
    PERCENTAGE("PERCENTAGE");
    private final String value;
}
