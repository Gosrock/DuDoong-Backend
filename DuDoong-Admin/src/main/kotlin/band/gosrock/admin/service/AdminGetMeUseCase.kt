package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.common.annotation.UseCase

@UseCase
class AdminGetMeUseCase(
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long): AdminUserDetailResponse {
        val user = adminAuthValidator.validateManagerOrAbove(userId)
        return AdminUserDetailResponse.from(user)
    }
}
