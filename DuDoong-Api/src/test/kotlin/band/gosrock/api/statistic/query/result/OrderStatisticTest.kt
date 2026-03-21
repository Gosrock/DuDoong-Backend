package band.gosrock.api.statistic.query.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class OrderStatisticTest {

    @Test
    fun `sellAmount가 null이면 0원으로 처리된다`() {
        val statistic = OrderStatistic(0L, 0L, null)

        assertEquals(0L, statistic.DoneCount)
        assertEquals(0L, statistic.notApprovedCount)
        assertEquals(0, statistic.sellAmount.amount.compareTo(BigDecimal.ZERO))
    }

    @Test
    fun `sellAmount가 정상값이면 Money로 변환된다`() {
        val statistic = OrderStatistic(5L, 2L, BigDecimal.valueOf(50000))

        assertEquals(5L, statistic.DoneCount)
        assertEquals(2L, statistic.notApprovedCount)
        assertEquals(50000L, statistic.sellAmount.longValue())
    }

    @Test
    fun `모든 값이 0이면 정상 생성된다`() {
        val statistic = OrderStatistic(0L, 0L, BigDecimal.ZERO)

        assertEquals(0L, statistic.DoneCount)
        assertEquals(0L, statistic.notApprovedCount)
        assertEquals(0L, statistic.sellAmount.longValue())
    }
}
