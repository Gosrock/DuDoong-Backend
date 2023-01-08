package band.gosrock.domain.domains.coupon.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DiscountType {
    // 정액 할인
    AMOUNT("AMOUNT"),
    // 정률 할인
    PERCENTAGE("PERCENTAGE");
    private String value;
}
