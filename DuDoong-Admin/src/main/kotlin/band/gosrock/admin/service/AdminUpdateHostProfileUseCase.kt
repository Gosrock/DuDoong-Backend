package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateHostProfileRequest
import band.gosrock.admin.model.dto.response.AdminHostDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostProfile
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateHostProfileUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
) {

    @Transactional
    fun execute(hostId: Long, request: AdminUpdateHostProfileRequest): AdminHostDetailResponse {
        val host = hostAdaptor.findById(hostId)
        val current = host.profile
        val updatedProfile = HostProfile(
            name = request.name ?: current?.name,
            introduce = request.introduce ?: current?.introduce,
            profileImageKey = current?.profileImage?.imageKey,
            contactEmail = request.contactEmail ?: current?.contactEmail,
            contactNumber = request.contactNumber ?: current?.contactNumber,
        )
        host.updateProfile(updatedProfile)
        hostRepository.save(host)
        return AdminHostDetailResponse.from(host)
    }
}
