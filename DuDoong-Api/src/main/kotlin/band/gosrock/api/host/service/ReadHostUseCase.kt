package band.gosrock.api.host.service

import band.gosrock.api.host.model.dto.response.HostDetailResponse
import band.gosrock.api.host.model.mapper.HostMapper
import band.gosrock.common.annotation.UseCase
import org.springframework.transaction.annotation.Transactional

@UseCase
class ReadHostUseCase(
    private val hostMapper: HostMapper,
) {
    @Transactional(readOnly = true)
    fun execute(hostId: Long): HostDetailResponse {
        return hostMapper.toHostDetailResponse(hostId)
    }
}
