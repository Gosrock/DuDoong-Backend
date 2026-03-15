package band.gosrock.domain.common.vo

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class DateTimePeriodTest {

    private val start: LocalDateTime = LocalDateTime.of(2025, 1, 1, 10, 0)
    private val end: LocalDateTime = LocalDateTime.of(2025, 1, 1, 18, 0)

    @Test
    fun `기간 내 시간은 contains가 true를 반환한다`() {
        val period = DateTimePeriod.between(start, end)
        val middle = LocalDateTime.of(2025, 1, 1, 14, 0)
        assertTrue(period.contains(middle))
    }

    @Test
    fun `시작 시각과 정확히 같으면 contains가 true를 반환한다`() {
        val period = DateTimePeriod.between(start, end)
        assertTrue(period.contains(start))
    }

    @Test
    fun `종료 시각과 정확히 같으면 contains가 true를 반환한다`() {
        val period = DateTimePeriod.between(start, end)
        assertTrue(period.contains(end))
    }

    @Test
    fun `시작 시각 이전이면 contains가 false를 반환한다`() {
        val period = DateTimePeriod.between(start, end)
        val before = LocalDateTime.of(2025, 1, 1, 9, 59)
        assertFalse(period.contains(before))
    }

    @Test
    fun `종료 시각 이후이면 contains가 false를 반환한다`() {
        val period = DateTimePeriod.between(start, end)
        val after = LocalDateTime.of(2025, 1, 1, 18, 1)
        assertFalse(period.contains(after))
    }

    @Test
    fun `startAt이 null이면 contains가 false를 반환한다`() {
        val period = DateTimePeriod.between(null, end)
        assertFalse(period.contains(start))
    }

    @Test
    fun `endAt이 null이면 contains가 false를 반환한다`() {
        val period = DateTimePeriod.between(start, null)
        assertFalse(period.contains(start))
    }

    @Test
    fun `startAt과 endAt이 모두 null이면 contains가 false를 반환한다`() {
        val period = DateTimePeriod.between(null, null)
        assertFalse(period.contains(LocalDateTime.now()))
    }
}
