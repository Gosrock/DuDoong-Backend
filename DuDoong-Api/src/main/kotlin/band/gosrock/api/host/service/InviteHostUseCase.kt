package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.request.InviteHostRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.service.HostService
import band.gosrock.domain.domains.user.adaptor.UserAdaptor

@UseCase
class InviteHostUseCase(
    private val userAdaptor: UserAdaptor,
    private val hostService: HostService,
    private val hostAdaptor: HostAdaptor,
    private val hostMapper: HostMapper,
) {
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, inviteHostRequest: InviteHostRequest): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        val invitedUser = userAdaptor.queryUserByEmail(inviteHostRequest.email)
        val invitedUserId = invitedUser.id!!
        val role = inviteHostRequest.role
        val hostUser = hostMapper.toHostUser(hostId, invitedUserId, role)

        return hostMapper.toHostDetailResponse(hostService.inviteHostUser(host, hostUser))
    }
}
