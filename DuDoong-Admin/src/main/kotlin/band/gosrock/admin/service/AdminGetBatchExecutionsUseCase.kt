package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminBatchExecutionResponse
import band.gosrock.admin.repository.AdminBatchExecutionQuery
import band.gosrock.common.annotation.UseCase
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetBatchExecutionsUseCase(
    private val adminBatchExecutionQuery: AdminBatchExecutionQuery,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long, jobName: String?, pageable: Pageable): Page<AdminBatchExecutionResponse> {
        adminAuthValidator.validateAdminOrAbove(userId)
        val page = PageRequest.of(pageable.pageNumber, minOf(pageable.pageSize, MAX_PAGE_SIZE))
        return adminBatchExecutionQuery.findExecutions(jobName?.takeIf { it.isNotBlank() }, page)
            .map { AdminBatchExecutionResponse.of(it) }
    }

    companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
