package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminTransferMasterRequest
import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminTransferMasterUseCase(
    private val adminAuthValidator: AdminAuthValidator,
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
) {

    @Transactional
    fun execute(adminUserId: Long, hostId: Long, request: AdminTransferMasterRequest): AdminHostDetailResponse {
        adminAuthValidator.validateAdminOrAbove(adminUserId)
        val host = hostAdaptor.findById(hostId)
        host.forceTransferMaster(request.newMasterUserId)
        hostRepository.save(host)
        return AdminHostDetailResponse.from(host)
    }
}
