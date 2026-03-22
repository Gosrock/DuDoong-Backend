package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserRoleChangeEvent(
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
    }

    override fun toString(): String = "HostUserRoleChangeEvent(hostId=$hostId, role=$role, userId=$userId)"
}
