package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateHostMemberRoleRequest
import band.gosrock.admin.model.dto.response.AdminHostMemberResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateHostMemberRoleUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val userAdaptor: UserAdaptor,
) {

    @Transactional
    fun execute(hostId: Long, userId: Long, request: AdminUpdateHostMemberRoleRequest): AdminHostMemberResponse {
        val host = hostAdaptor.findById(hostId)
        // 어드민이므로 마스터 권한 체크 건너뜀
        host.setHostUserRole(userId, request.role)
        hostRepository.save(host)

        val hostUser = host.getHostUserByUserId(userId)
        val userName = runCatching { userAdaptor.queryUser(userId).profile?.name }.getOrNull()
        return AdminHostMemberResponse.of(hostUser, userName)
    }
}
