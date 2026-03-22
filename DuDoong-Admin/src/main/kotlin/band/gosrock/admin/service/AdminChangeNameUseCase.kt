package band.gosrock.admin.service

import band.gosrock.admin.model.dto.request.AdminChangeNameRequest
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class AdminChangeNameUseCase(
    private val adminAuthValidator: AdminAuthValidator,
    private val userAdaptor: UserAdaptor,
) {

    @Transactional
    fun execute(adminUserId: Long, targetUserId: Long, request: AdminChangeNameRequest) {
        adminAuthValidator.validateAdminOrAbove(adminUserId)
        val user = userAdaptor.queryUser(targetUserId)
        user.changeName(request.name)
    }
}
