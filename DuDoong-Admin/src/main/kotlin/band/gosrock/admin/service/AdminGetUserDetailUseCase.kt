package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetUserDetailUseCase(
    private val userAdaptor: UserAdaptor,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, targetUserId: Long): AdminUserDetailResponse {
        adminAuthValidator.validateAdminOrAbove(userId)
        val user = userAdaptor.queryUser(targetUserId)
        return AdminUserDetailResponse.from(user)
    }
}
