package band.gosrock.api.admin.service

import band.gosrock.api.admin.model.dto.request.AdminUpdateUserStatusRequest
import band.gosrock.api.admin.model.dto.response.AdminUserResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminUpdateUserStatusUseCase(
    private val userAdaptor: UserAdaptor,
    private val userRepository: UserRepository,
) {

    @Transactional
    fun execute(targetUserId: Long, request: AdminUpdateUserStatusRequest): AdminUserResponse {
        val targetUser = userAdaptor.queryUser(targetUserId)

        val field = targetUser.javaClass.getDeclaredField("accountState")
        field.isAccessible = true
        field.set(targetUser, request.status)

        userRepository.save(targetUser)
        return AdminUserResponse.from(targetUser)
    }
}
