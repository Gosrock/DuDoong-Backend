package band.gosrock.api.user.service

import band.gosrock.api.user.model.dto.request.ChangeNameRequest
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.adaptor.UserAdaptor
import org.springframework.transaction.annotation.Transactional

@UseCase
class ChangeNameUseCase(
    private val userAdaptor: UserAdaptor,
) {

    @Transactional
    fun execute(userId: Long, request: ChangeNameRequest) {
        val user = userAdaptor.queryUser(userId)
        user.changeName(request.name)
    }
}
