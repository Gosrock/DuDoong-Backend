package band.gosrock.domain.common.vo

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MoneyTest {

    @Test
    fun `ZERO는 0원이다`() {
        assertEquals(0L, Money.ZERO.longValue())
    }

    @Test
    fun `wons(Long)으로 금액을 생성한다`() {
        val money = Money.wons(5000L)
        assertEquals(5000L, money.longValue())
    }

    @Test
    fun `wons(Double)으로 금액을 생성한다`() {
        val money = Money.wons(1500.0)
        assertEquals(1500L, money.longValue())
    }

    @Test
    fun `두 금액을 더하면 합산된 금액이 반환된다`() {
        val a = Money.wons(3000L)
        val b = Money.wons(2000L)
        assertEquals(Money.wons(5000L), a.plus(b))
    }

    @Test
    fun `두 금액을 빼면 차감된 금액이 반환된다`() {
        val a = Money.wons(5000L)
        val b = Money.wons(2000L)
        assertEquals(Money.wons(3000L), a.minus(b))
    }

    @Test
    fun `금액에 배수를 곱하면 곱셈 결과가 반환된다`() {
        val a = Money.wons(1000L)
        assertEquals(Money.wons(2000L), a.times(2.0))
    }

    @Test
    fun `금액을 나누면 나눗셈 결과가 반환된다`() {
        val a = Money.wons(6000L)
        assertEquals(Money.wons(2000L), a.divide(3.0))
    }

    @Test
    fun `작은 금액은 큰 금액보다 isLessThan이 true다`() {
        val small = Money.wons(100L)
        val big = Money.wons(200L)
        assertTrue(small.isLessThan(big))
    }

    @Test
    fun `큰 금액은 작은 금액보다 isLessThan이 false다`() {
        val small = Money.wons(100L)
        val big = Money.wons(200L)
        assertFalse(big.isLessThan(small))
    }

    @Test
    fun `같은 금액끼리 isLessThanOrEqual이 true다`() {
        val a = Money.wons(500L)
        val b = Money.wons(500L)
        assertTrue(a.isLessThanOrEqual(b))
    }

    @Test
    fun `큰 금액은 작은 금액보다 isGreaterThan이 true다`() {
        val big = Money.wons(300L)
        val small = Money.wons(100L)
        assertTrue(big.isGreaterThan(small))
    }

    @Test
    fun `같은 금액끼리 isGreaterThanOrEqual이 true다`() {
        val a = Money.wons(1000L)
        assertTrue(a.isGreaterThanOrEqual(a))
    }

    @Test
    fun `같은 금액끼리 equals가 true다`() {
        val a = Money.wons(1000L)
        val b = Money.wons(1000L)
        assertEquals(a, b)
    }

    @Test
    fun `다른 금액끼리 equals가 false다`() {
        val a = Money.wons(1000L)
        val b = Money.wons(2000L)
        assertNotEquals(a, b)
    }

    @Test
    fun `같은 금액은 hashCode가 같다`() {
        val a = Money.wons(1000L)
        val b = Money.wons(1000L)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `sum은 컬렉션 원소들의 금액을 모두 더한다`() {
        val prices = listOf(Money.wons(1000L), Money.wons(2000L), Money.wons(3000L))
        val total = Money.sum(prices) { it }
        assertEquals(Money.wons(6000L), total)
    }

    @Test
    fun `빈 컬렉션의 sum은 ZERO다`() {
        val total = Money.sum(emptyList<Money>()) { it }
        assertEquals(Money.ZERO, total)
    }

    @Test
    fun `getDiscountAmountByPercentage는 공급가에 퍼센트 비율을 적용한 할인금액을 반환한다`() {
        val supply = Money.wons(10000L)
        val discountMoney = Money.ZERO
        val discountAmount = discountMoney.getDiscountAmountByPercentage(supply, 10L)
        assertEquals(1000L, discountAmount)
    }

    @Test
    fun `toString은 원 단위 문자열을 반환한다`() {
        val money = Money.wons(5000L)
        assertEquals("5000원", money.toString())
    }

    @Test
    fun `ZERO에서 ZERO를 더하면 ZERO다`() {
        assertEquals(Money.ZERO, Money.ZERO.plus(Money.ZERO))
    }
}
