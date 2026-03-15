package band.gosrock.domain.common.vo

import band.gosrock.common.annotation.DateFormat
import band.gosrock.domain.domains.event.domain.Event
import java.time.LocalDateTime

data class EventBasicVo(
    val name: String? = null,
    @DateFormat val startAt: LocalDateTime? = null,
    @DateFormat val endAt: LocalDateTime? = null,
    val runTime: Long? = null,
) {
    companion object {
        @JvmStatic
        fun from(event: Event): EventBasicVo {
            val eventBasic = event.eventBasic ?: return EventBasicVo()
            return EventBasicVo(
                name = eventBasic.name,
                startAt = eventBasic.startAt,
                endAt = event.getEndAt(),
                runTime = eventBasic.runTime,
            )
        }
    }
}
