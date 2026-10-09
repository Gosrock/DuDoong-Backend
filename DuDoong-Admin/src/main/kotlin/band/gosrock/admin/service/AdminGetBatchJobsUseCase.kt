package band.gosrock.admin.service

import band.gosrock.admin.model.dto.response.AdminBatchJobSummaryResponse
import band.gosrock.admin.repository.AdminBatchExecutionQuery
import band.gosrock.common.annotation.UseCase
import java.time.LocalDateTime
import java.time.ZoneId
import org.springframework.transaction.annotation.Transactional

@UseCase
@Transactional(readOnly = true)
class AdminGetBatchJobsUseCase(
    private val adminBatchExecutionQuery: AdminBatchExecutionQuery,
    private val adminAuthValidator: AdminAuthValidator,
) {

    fun execute(userId: Long): List<AdminBatchJobSummaryResponse> {
        adminAuthValidator.validateAdminOrAbove(userId)
        // 배치 메타 테이블 시각은 KST 로 기록된다
        val failureSince = LocalDateTime.now(ZoneId.of("Asia/Seoul")).minusDays(RECENT_FAILURE_DAYS)
        return adminBatchExecutionQuery.findJobSummaries(failureSince)
            .sortedWith(compareByDescending(nullsFirst()) { it.lastStartTime })
            .map { AdminBatchJobSummaryResponse.of(it) }
    }

    companion object {
        const val RECENT_FAILURE_DAYS = 7L
    }
}
