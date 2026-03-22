package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminAddHostMemberRequest
import band.gosrock.admin.model.dto.response.AdminHostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.domain.HostUser
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminAddHostMemberUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val userAdaptor: UserAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, hostId: Long, request: AdminAddHostMemberRequest): AdminHostMemberResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val host = hostAdaptor.findById(hostId)
        val hostUser = HostUser(host, request.userId, request.role)
        host.addHostUsers(setOf(hostUser))
        hostRepository.save(host)

        val userName = runCatching { userAdaptor.queryUser(request.userId).profile?.name }.getOrNull()
        val savedHostUser = host.getHostUserByUserId(request.userId)
        return AdminHostMemberResponse.of(savedHostUser, userName)
    }
}
