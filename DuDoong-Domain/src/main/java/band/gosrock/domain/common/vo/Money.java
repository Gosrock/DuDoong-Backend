package band.gosrock.domain.common.vo;


import band.gosrock.domain.common.converter.BigDecimalScale6WithBankersRoundingConverter;
import com.fasterxml.jackson.annotation.JsonValue;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@Embeddable
@RequiredArgsConstructor
public class Money {
    public static final Money ZERO = Money.wons(0);

    // DECIMAL(21,6) 타입에 대한 맵핑 상세 정의
    // 숫자 범위는 -999999999999999.999999 ~ +999999999999999.999999
    @Column(name = "amount", nullable = false, precision = 21, scale = 6)
    @Convert(converter = BigDecimalScale6WithBankersRoundingConverter.class)
    private final BigDecimal amount;

    public Money() {
        amount = new BigDecimal(0);
    }

    public static Money wons(long amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static Money wons(double amount) {
        return new Money(BigDecimal.valueOf(amount));
    }

    public static <T> Money sum(Collection<T> bags, Function<T, Money> monetary) {
        return bags.stream().map(monetary::apply).reduce(Money.ZERO, Money::plus);
    }

    //    public Money(BigDecimal amount) {
    //        this.amount = amount;
    //    }

    public Money plus(Money amount) {
        return new Money(this.amount.add(amount.amount));
    }

    public Money minus(Money amount) {
        return new Money(this.amount.subtract(amount.amount));
    }

    public Money times(double percent) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(percent)));
    }

    public Money divide(double divisor) {
        return new Money(amount.divide(BigDecimal.valueOf(divisor)));
    }

    public boolean isLessThan(Money other) {
        return amount.compareTo(other.amount) < 0;
    }

    public boolean isGreaterThanOrEqual(Money other) {
        return amount.compareTo(other.amount) >= 0;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Long longValue() {
        return amount.longValue();
    }

    public Double doubleValue() {
        return amount.doubleValue();
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof Money)) {
            return false;
        }

        Money other = (Money) object;
        return Objects.equals(amount.doubleValue(), other.amount.doubleValue());
    }

    public int hashCode() {
        return Objects.hashCode(amount);
    }

    @JsonValue
    public String toString() {
        return amount.longValue() + "원";
    }

    public long getDiscountAmountByPercentage(Money supply, Long percentage) {
        double discountPercent = percentage * 0.01;
        // 할인 퍼센트 적용한 값 반올림 처리
        return Math.round(supply.times(discountPercent).longValue());
    }
}
