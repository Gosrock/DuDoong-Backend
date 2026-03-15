package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.event.domain.Event

data class EventDeletionEvent(
    val hostId: Long,
    val eventName: String,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(event: Event): EventDeletionEvent =
            EventDeletionEvent(
                hostId = event.hostId ?: 0L,
                eventName = event.eventBasic?.name ?: "",
            )
    }
}
