package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.common.vo.HostProfileVo
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserInvitationEvent(
    val userId: Long?,
    val role: HostRole?,
    val hostProfileVo: HostProfileVo?,
) : DomainEvent() {
    companion object {
        @JvmStatic
        fun of(host: Host, hostUser: HostUser): HostUserInvitationEvent =
            HostUserInvitationEvent(
                hostProfileVo = host.toHostProfileVo(),
                role = hostUser.role,
                userId = hostUser.userId,
            )
    }

    override fun toString(): String = "HostUserInvitationEvent(userId=$userId, role=$role)"
}
