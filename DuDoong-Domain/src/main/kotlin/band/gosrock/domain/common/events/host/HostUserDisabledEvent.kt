package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserDisabledEvent(
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
    }

    override fun toString(): String = "HostUserDisabledEvent(hostId=$hostId, userId=$userId)"
}
