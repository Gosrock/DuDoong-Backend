package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminUserResponse
import band.gosrock.common.annotation.UseCase
import band.gosrock.domain.domains.user.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetUsersUseCase(
    private val userRepository: UserRepository,
) {

    fun execute(keyword: String?, pageable: Pageable): Page<AdminUserResponse> {
        return userRepository.findAllByKeyword(keyword, pageable)
            .map { AdminUserResponse.from(it) }
    }
}
