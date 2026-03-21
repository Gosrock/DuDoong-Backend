package band.gosrock.api.statistic.query.result

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IssuedTicketStatisticTest {

    @Test
    fun `발급 0건 입장 0건이면 미입장도 0건이다`() {
        val statistic = IssuedTicketStatistic(0L, 0L)

        assertEquals(0L, statistic.issuedCount)
        assertEquals(0L, statistic.enteredCount)
        assertEquals(0L, statistic.notEnteredCount)
    }

    @Test
    fun `발급 10건 입장 3건이면 미입장 7건이다`() {
        val statistic = IssuedTicketStatistic(10L, 3L)

        assertEquals(10L, statistic.issuedCount)
        assertEquals(3L, statistic.enteredCount)
        assertEquals(7L, statistic.notEnteredCount)
    }

    @Test
    fun `전원 입장하면 미입장 0건이다`() {
        val statistic = IssuedTicketStatistic(5L, 5L)

        assertEquals(5L, statistic.issuedCount)
        assertEquals(5L, statistic.enteredCount)
        assertEquals(0L, statistic.notEnteredCount)
    }
}
