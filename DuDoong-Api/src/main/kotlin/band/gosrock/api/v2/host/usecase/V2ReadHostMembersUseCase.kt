package band.gosrock.api.v2.host.usecase

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.v2.host.dto.response.V2HostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.Host
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class V2ReadHostMembersUseCase(
    private val hostAdaptor: HostAdaptor,
    private val userAdaptor: UserAdaptor,
) {
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long): List<V2HostMemberResponse> =
        toMemberResponses(hostAdaptor.findById(hostId))

    /** 활성 멤버만, 마스터 → 매니저 → 일반 순, 같은 역할은 먼저 들어온 순 */
    fun toMemberResponses(host: Host): List<V2HostMemberResponse> {
        val hostUsers = host.getActiveHostUsers()
            .sortedWith(compareBy({ it.role.ordinal }, { it.createdAt }, { it.id }))
        val users = userAdaptor.queryUserListByIdIn(hostUsers.mapNotNull { it.userId }).associateBy { it.id }
        return hostUsers.map { V2HostMemberResponse.of(it, users[it.userId]) }
    }
}
