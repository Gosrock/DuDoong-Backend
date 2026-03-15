package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

data class EventCreationEvent(
    val hostId: Long?,
    val eventName: String?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(hostId: Long?, eventName: String?): EventCreationEvent =
            EventCreationEvent(hostId = hostId, eventName = eventName)
    }
}
