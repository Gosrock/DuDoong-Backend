package band.gosrock.api.host.service

import band.gosrock.api.common.aop.hostRole.FindHostFrom.HOST_ID
import band.gosrock.api.common.aop.hostRole.HostQualification.GUEST
import band.gosrock.api.common.aop.hostRole.HostRolesAllowed
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadHostUseCase(
    private val hostMapper: HostMapper,
) {
    @Transactional(readOnly = true)
    @HostRolesAllowed(role = GUEST, findHostFrom = HOST_ID)
    fun execute(userId: Long, hostId: Long): HostDetailResponse {
        return hostMapper.toHostDetailResponse(hostId, userId)
    }
}
