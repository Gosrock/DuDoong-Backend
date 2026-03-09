package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host

class HostRegisterSlackEvent private constructor(
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

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var hostName: String? = null
        fun hostId(v: Long?) = apply { hostId = v }
        fun hostName(v: String?) = apply { hostName = v }
        fun build() = HostRegisterSlackEvent(hostId, hostName)
    }

    override fun toString(): String = "HostRegisterSlackEvent(hostId=$hostId, hostName=$hostName)"
}
