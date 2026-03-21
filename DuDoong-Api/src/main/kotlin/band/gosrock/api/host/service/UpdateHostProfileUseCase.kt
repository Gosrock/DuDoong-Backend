package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.MANAGER
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.request.UpdateHostRequest
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class UpdateHostProfileUseCase(
    private val hostService: HostService,
    private val hostAdaptor: HostAdaptor,
    private val hostMapper: HostMapper,
) {
    @Transactional
    @HostRolesAllowed(role = MANAGER, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long, updateHostRequest: UpdateHostRequest): HostDetailResponse {
        val host = hostAdaptor.findById(hostId)

        return hostMapper.toHostDetailResponse(
            hostService.updateHostProfile(host, hostMapper.toHostProfile(updateHostRequest))
        )
    }
}
