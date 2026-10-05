package band.gosrock.domain.common.events.event

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.event.domain.Event

/** 호스트 공연 삭제 (v1·v2, `Event.deleteSoft`). [eventId] 는 선물 연쇄용 (#734) */
data class EventDeletionEvent(
    val hostId: Long,
    val eventName: String,
    val eventId: Long? = null,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(event: Event): EventDeletionEvent =
            EventDeletionEvent(
                hostId = event.hostId ?: 0L,
                eventName = event.eventBasic?.name ?: "",
                eventId = event.id,
            )
    }
}
