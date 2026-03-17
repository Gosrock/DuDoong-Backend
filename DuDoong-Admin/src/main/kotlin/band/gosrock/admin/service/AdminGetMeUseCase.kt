package band.gosrock.admin.service

import band.gosrock.admin.exception.AdminForbiddenException
import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.domain.AccountRole

@UseCase
class AdminGetMeUseCase(
    private val userAdaptor: UserAdaptor,
) {

    fun execute(userId: Long): AdminUserDetailResponse {
        val user = userAdaptor.queryUser(userId)
        if (user.accountRole == AccountRole.USER) {
            throw AdminForbiddenException.EXCEPTION
        }
        return AdminUserDetailResponse.from(user)
    }
}
