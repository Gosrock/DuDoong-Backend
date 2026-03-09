package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserRoleChangeEvent private constructor(
    val hostId: Long?,
    val hostName: String?,
    val role: HostRole?,
    val userId: Long?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(host: Host, hostUser: HostUser): HostUserRoleChangeEvent =
            HostUserRoleChangeEvent(
                hostId = host.id,
                hostName = host.toHostProfileVo().name,
                role = hostUser.role,
                userId = hostUser.userId,
            )

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var hostId: Long? = null
        private var hostName: String? = null
        private var role: HostRole? = null
        private var userId: Long? = null
        fun hostId(v: Long?) = apply { hostId = v }
        fun hostName(v: String?) = apply { hostName = v }
        fun role(v: HostRole?) = apply { role = v }
        fun userId(v: Long?) = apply { userId = v }
        fun build() = HostUserRoleChangeEvent(hostId, hostName, role, userId)
    }

    override fun toString(): String = "HostUserRoleChangeEvent(hostId=$hostId, role=$role, userId=$userId)"
}
