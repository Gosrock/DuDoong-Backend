package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

class HostUserJoinEvent(
    val hostId: Long?,
    val userId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(hostId: Long?, userId: Long?): HostUserJoinEvent =
            HostUserJoinEvent(hostId, userId)
    }

    override fun toString(): String = "HostUserJoinEvent(hostId=$hostId, userId=$userId)"
}
