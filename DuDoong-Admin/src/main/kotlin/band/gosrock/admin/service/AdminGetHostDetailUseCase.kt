package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetHostDetailUseCase(
    private val hostAdaptor: HostAdaptor,
) {

    fun execute(hostId: Long): AdminHostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        return AdminHostDetailResponse.from(host)
    }
}
