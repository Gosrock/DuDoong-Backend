package band.gosrock.api.host.service

import band.gosrock.api.common.UserUtils
import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.service.HostService
import org.springframework.transaction.annotation.Transactional

@UseCase
class JoinHostUseCase(
    private val userUtils: UserUtils,
    private val hostService: HostService,
    private val hostAdaptor: HostAdaptor,
    private val hostMapper: HostMapper,
) {
    @Transactional
    fun execute(hostId: Long): HostDetailResponse {
        val userId = userUtils.getCurrentUserId()
        val host = hostAdaptor.findById(hostId)

        return hostMapper.toHostDetailResponse(hostService.activateHostUser(host, userId))
    }
}
