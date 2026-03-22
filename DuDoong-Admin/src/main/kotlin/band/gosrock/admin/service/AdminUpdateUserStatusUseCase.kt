package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.admin.model.dto.response.AdminUserResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateUserStatusUseCase(
    private val userAdaptor: UserAdaptor,
    private val userRepository: UserRepository,
    private val adminAuthValidator: AdminAuthValidator,
) {

    @Transactional
    fun execute(userId: Long, targetUserId: Long, request: AdminUpdateUserStatusRequest): AdminUserResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val targetUser = userAdaptor.queryUser(targetUserId)
        targetUser.changeAccountState(request.status)
        userRepository.save(targetUser)
        return AdminUserResponse.from(targetUser)
    }
}
