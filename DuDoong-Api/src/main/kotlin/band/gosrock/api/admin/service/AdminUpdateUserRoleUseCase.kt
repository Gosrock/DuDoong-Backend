package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.request.AdminUpdateUserRoleRequest
import band.gosrock.api.admin.model.dto.response.AdminUserResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.security.access.AccessDeniedException
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateUserRoleUseCase(
    private val userAdaptor: UserAdaptor,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun execute(currentUserId: Long, targetUserId: Long, request: AdminUpdateUserRoleRequest): AdminUserResponse {
        val currentUser = userAdaptor.queryUser(currentUserId)
        if (currentUser.accountRole != AccountRole.SUPER_ADMIN) {
            throw AccessDeniedException("SUPER_ADMIN 권한이 필요합니다.")
        }

        val targetUser = userAdaptor.queryUser(targetUserId)
        // Use reflection-like approach since accountRole has protected setter
        // We need to add a method to User entity or use repository directly
        val field = targetUser.javaClass.getDeclaredField("accountRole")
        field.isAccessible = true
        field.set(targetUser, request.role)

        userRepository.save(targetUser)
        return AdminUserResponse.from(targetUser)
    }
}
