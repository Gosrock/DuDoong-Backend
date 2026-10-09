package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.response.InviteUserResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadInviteUsersUseCase(
    private val hostMapper: HostMapper,
) {
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, email: String): InviteUserResponse {
        return hostMapper.toHostInviteUserList(hostId, email)
    }
}
