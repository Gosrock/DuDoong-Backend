package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserDisabledEvent private constructor(
    val hostId: Long?,
    val hostName: String?,
    val userId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(host: Host, hostUser: HostUser): HostUserDisabledEvent =
            HostUserDisabledEvent(
                hostId = host.id,
                hostName = host.toHostProfileVo().name,
                userId = hostUser.userId,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var hostName: String? = null
        private var userId: Long? = null
        fun hostId(v: Long?) = apply { hostId = v }
        fun hostName(v: String?) = apply { hostName = v }
        fun userId(v: Long?) = apply { userId = v }
        fun build() = HostUserDisabledEvent(hostId, hostName, userId)
    }

    override fun toString(): String = "HostUserDisabledEvent(hostId=$hostId, userId=$userId)"
}
