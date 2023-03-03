package band.gosrock.domain.domains.coupon.domain;


import band.gosrock.common.annotation.EnumClass;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EnumClass
public enum ApplyTarget {
    ALL("ALL"),
    SUB("SUB");
    private final String value;
}
