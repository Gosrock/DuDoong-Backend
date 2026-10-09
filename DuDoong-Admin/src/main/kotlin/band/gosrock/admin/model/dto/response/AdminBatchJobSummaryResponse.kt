package band.gosrock.admin.model.dto.response

import band.gosrock.admin.repository.BatchJobSummaryRow
import java.time.LocalDateTime

data class AdminBatchJobSummaryResponse(
    val jobName: String,
    val lastExecutionId: Long,
    val lastStatus: String,
    val lastStartTime: LocalDateTime?,
    val lastEndTime: LocalDateTime?,
    val lastSuccessTime: LocalDateTime?,
    val recentFailureCount: Long,
) {
    companion object {
        fun of(row: BatchJobSummaryRow): AdminBatchJobSummaryResponse =
            AdminBatchJobSummaryResponse(
                jobName = row.jobName,
                lastExecutionId = row.lastExecutionId,
                lastStatus = row.lastStatus,
                lastStartTime = row.lastStartTime,
                lastEndTime = row.lastEndTime,
                lastSuccessTime = row.lastSuccessTime,
                recentFailureCount = row.recentFailureCount,
            )
    }
}
