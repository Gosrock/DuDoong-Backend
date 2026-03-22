package band.gosrock.domain.common.vo

import java.time.LocalDateTime
import jakarta.persistence.Embeddable

@Embeddable
open class DateTimePeriod(
    // 쿠폰 발행 시작 시각
    var startAt: LocalDateTime? = null,
    // 쿠폰 발행 마감 시각
    var endAt: LocalDateTime? = null,
) {
    fun contains(datetime: LocalDateTime): Boolean {
        val start = startAt ?: return false
        val end = endAt ?: return false
        return (datetime.isAfter(start) || datetime == start) &&
            (datetime.isBefore(end) || datetime == end)
    }

    companion object {
        @JvmStatic
        fun between(startAt: LocalDateTime?, endAt: LocalDateTime?): DateTimePeriod =
            DateTimePeriod(startAt, endAt)
    }
}
