package band.gosrock.admin.service

import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.host.adaptor.HostAdaptor
import band.gosrock.domain.domains.host.repository.HostRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminRemoveHostMemberUseCase(
    private val hostAdaptor: HostAdaptor,
    private val hostRepository: HostRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, hostId: Long, targetUserId: Long) {
        adminAuthValidator.validateAdminOrAbove(userId)
        val host = hostAdaptor.findById(hostId)
        // 어드민이므로 active 여부 체크 없이 강제 제거
        val hostUser = host.getHostUserByUserId(targetUserId)
        host.hostUsers.remove(hostUser)
        hostRepository.save(host)
    }
}
