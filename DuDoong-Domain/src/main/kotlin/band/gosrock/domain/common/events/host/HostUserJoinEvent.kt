package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent

class HostUserJoinEvent private constructor(
    val hostId: Long?,
    val userId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(hostId: Long?, userId: Long?): HostUserJoinEvent =
            HostUserJoinEvent(hostId, userId)

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var userId: Long? = null
        fun hostId(v: Long?) = apply { hostId = v }
        fun userId(v: Long?) = apply { userId = v }
        fun build() = HostUserJoinEvent(hostId, userId)
    }

    override fun toString(): String = "HostUserJoinEvent(hostId=$hostId, userId=$userId)"
}
