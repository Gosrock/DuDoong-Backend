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
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun executeAll(userId: Long, keyword: String?): List<AdminUserResponse> {
        adminAuthValidator.validateManagerOrAbove(userId)
        return userRepository.findAllByKeywordNoPage(keyword)
            .map { AdminUserResponse.from(it) }
    }

    fun execute(userId: Long, keyword: String?, pageable: Pageable): Page<AdminUserResponse> {
        adminAuthValidator.validateManagerOrAbove(userId)
        return userRepository.findAllByKeyword(keyword, pageable)
            .map { AdminUserResponse.from(it) }
    }
}
