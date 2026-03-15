package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.event.domain.Event

data class EventContentChangeEvent(
    val hostId: Long,
    val eventId: Long,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(event: Event): EventContentChangeEvent =
            EventContentChangeEvent(
                hostId = event.hostId ?: 0L,
                eventId = event.id ?: 0L,
            )
    }
}
