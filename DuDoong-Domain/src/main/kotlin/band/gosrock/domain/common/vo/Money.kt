package band.gosrock.domain.common.vo

import band.gosrock.domain.common.converter.BigDecimalScale6WithBankersRoundingConverter
import com.fasterxml.jackson.annotation.JsonValue
import java.math.BigDecimal
import java.util.Objects
import java.util.function.Function
import javax.persistence.Column
import javax.persistence.Convert
import javax.persistence.Embeddable

@Embeddable
class Money() {
    // DECIMAL(21,6) 타입에 대한 맵핑 상세 정의
    @Column(name = "amount", nullable = false, precision = 21, scale = 6)
    @Convert(converter = BigDecimalScale6WithBankersRoundingConverter::class)
    var amount: BigDecimal = BigDecimal.ZERO
        protected set

    constructor(amount: BigDecimal) : this() {
        this.amount = amount
    }

    companion object {
        @JvmField
        val ZERO: Money = Money.wons(0)

        @JvmStatic
        fun wons(amount: Long): Money = Money(BigDecimal.valueOf(amount))

        @JvmStatic
        fun wons(amount: Double): Money = Money(BigDecimal.valueOf(amount))

        @JvmStatic
        fun <T> sum(bags: Collection<T>, monetary: Function<T, Money>): Money =
            bags.stream().map(monetary).reduce(ZERO, Money::plus)
    }

    fun plus(amount: Money): Money = Money(this.amount.add(amount.amount))
    fun minus(amount: Money): Money = Money(this.amount.subtract(amount.amount))
    fun times(percent: Double): Money = Money(this.amount.multiply(BigDecimal.valueOf(percent)))
    fun divide(divisor: Double): Money = Money(amount.divide(BigDecimal.valueOf(divisor)))

    fun isLessThan(other: Money): Boolean = amount.compareTo(other.amount) < 0
    fun isLessThanOrEqual(other: Money): Boolean = amount.compareTo(other.amount) <= 0
    fun isGreaterThanOrEqual(other: Money): Boolean = amount.compareTo(other.amount) >= 0
    fun isGreaterThan(other: Money): Boolean = amount.compareTo(other.amount) > 0

    fun longValue(): Long = amount.toLong()
    fun doubleValue(): Double = amount.toDouble()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Money) return false
        return Objects.equals(amount.toDouble(), other.amount.toDouble())
    }

    override fun hashCode(): Int = Objects.hashCode(amount)

    @JsonValue
    override fun toString(): String = "${amount.toLong()}원"

    fun getDiscountAmountByPercentage(supply: Money, percentage: Long): Long {
        val discountPercent = percentage * 0.01
        return Math.round(supply.times(discountPercent).longValue().toDouble())
    }
}
