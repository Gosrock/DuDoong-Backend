package band.gosrock.domain.common.vo;


import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Money {
    @Column(nullable = false)
    private Long value;

    public Money(Long value) {
        this.value = value;
    }

    public static Money from(Long value) {
        return new Money(value);
    }

    public Money add(Money money) {
        return new Money(this.value + money.value);
    }

    public Money multiply(Long multiplier) {
        return new Money(value * multiplier);
    }
}
