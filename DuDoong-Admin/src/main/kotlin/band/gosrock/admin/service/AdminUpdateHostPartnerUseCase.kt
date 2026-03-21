package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateHostPartnerRequest
import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateHostPartnerUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, hostId: Long, request: AdminUpdateHostPartnerRequest): AdminHostDetailResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val host = hostAdaptor.findById(hostId)
        host.changePartner(request.partner)
        hostRepository.save(host)
        return AdminHostDetailResponse.from(host)
    }
}
