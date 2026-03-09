package band.gosrock.domain.common.events.host

import band.gosrock.domain.common.aop.domainEvent.DomainEvent
import band.gosrock.domain.common.vo.HostProfileVo
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.host.domain.HostRole
import band.gosrock.domain.domains.host.domain.HostUser

class HostUserInvitationEvent private constructor(
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

        @JvmStatic
        fun builder() = Builder()
    }

    class Builder {
        private var userId: Long? = null
        private var role: HostRole? = null
        private var hostProfileVo: HostProfileVo? = null
        fun userId(v: Long?) = apply { userId = v }
        fun role(v: HostRole?) = apply { role = v }
        fun hostProfileVo(v: HostProfileVo?) = apply { hostProfileVo = v }
        fun build() = HostUserInvitationEvent(userId, role, hostProfileVo)
    }

    override fun toString(): String = "HostUserInvitationEvent(userId=$userId, role=$role)"
}
