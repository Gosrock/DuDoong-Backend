package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminUserDetailResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetUserDetailUseCase(
    private val userAdaptor: UserAdaptor,
) {

    fun execute(userId: Long): AdminUserDetailResponse {
        val user = userAdaptor.queryUser(userId)
        return AdminUserDetailResponse.from(user)
    }
}
