package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host

class HostRegisterSlackEvent(
    val hostId: Long?,
    val hostName: String?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(host: Host): HostRegisterSlackEvent =
            HostRegisterSlackEvent(
                hostId = host.id,
                hostName = host.toHostProfileVo().name,
            )
    }

    override fun toString(): String = "HostRegisterSlackEvent(hostId=$hostId, hostName=$hostName)"
}
