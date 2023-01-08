package band.gosrock.domain.domains.coupon.domain;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ApplyTarget {
    ALL("ALL"),
    SUB("SUB");
    private String value;
}
